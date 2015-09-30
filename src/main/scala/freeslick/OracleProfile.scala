package freeslick

import java.sql.{ PreparedStatement, Blob }
import java.util.UUID

import com.typesafe.config.{ Config, ConfigException }
import slick.dbio.{ Effect, NoStream, DBIO }
import slick.driver._
import slick.jdbc.meta._
import slick.jdbc._
import slick.lifted._
import slick.model
import slick.compiler._

import scala.concurrent.ExecutionContext
import slick.ast._
import slick.util.MacroSupport.macroSupportInterpolation
import slick.profile.{ RelationalProfile, Capability }
import slick.compiler.CompilerState

/**
 * Slick profile for Oracle.
 *
 * This profile implements the `scala.slick.driver.JdbcProfile`
 * ''without'' the following capabilities:
 *
 *    - JdbcProfile.capabilities.forceInsert
 *    - JdbcProfile.capabilities.returnInsertKey
 *    - JdbcProfile.capabilities.booleanMetaData
 *    - JdbcProfile.capabilities.supportsByte
 * - JdbcProfile.capabilities.distinguishesIntTypes
 *
 */
trait OracleProfile extends JdbcDriver {
  driver =>

  override protected def computeCapabilities: Set[Capability] = (super.computeCapabilities
    //In 11g AutoInc columns are simulated by Sequences, so no forceInsert or returnInsertKey
    - JdbcProfile.capabilities.forceInsert
    - JdbcProfile.capabilities.returnInsertKey
    - JdbcProfile.capabilities.booleanMetaData
    - JdbcProfile.capabilities.supportsByte
    - JdbcProfile.capabilities.distinguishesIntTypes
  )

  override protected def computeQueryCompiler =
    super.computeQueryCompiler.addAfter(new FreeslickRewriteBooleans, QueryCompiler.relationalPhases.last)

  override val columnTypes = new JdbcTypes

  override def createModelBuilder(tables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext): JdbcModelBuilder =
    new ModelBuilder(tables, ignoreInvalidDefaults)

  override def createQueryBuilder(n: Node, state: CompilerState): QueryBuilder = new QueryBuilder(n, state)

  override def createColumnDDLBuilder(column: FieldSymbol, table: Table[_]): ColumnDDLBuilder = new ColumnDDLBuilder(column)

  override def createTableDDLBuilder(table: Table[_]): TableDDLBuilder = new TableDDLBuilder(table)

  override def createSequenceDDLBuilder(seq: Sequence[_]): SequenceDDLBuilder[_] = new SequenceDDLBuilder(seq)

  override def createSchemaActionExtensionMethods(schema: SchemaDescription): SchemaActionExtensionMethods =
    new SchemaActionExtensionMethodsImpl(schema)

  override def createDDLInvoker(ddl: SchemaDescription) = new DDLInvoker(ddl)

  override def defaultTables(implicit ec: ExecutionContext): DBIO[Seq[MTable]] = {
    import driver.api._
    val userQ = Functions.user.result
    userQ.flatMap(user => MTable.getTables(None, Some(user), None, Some(Seq("TABLE"))))
  }

  override def defaultSqlTypeName(tmd: JdbcType[_], size: Option[RelationalProfile.ColumnOption.Length]): String = tmd.sqlType match {
    case java.sql.Types.TIME => "DATE"
    case java.sql.Types.DOUBLE => "DOUBLE PRECISION"
    case java.sql.Types.BIGINT => "NUMBER(19)"
    case java.sql.Types.TINYINT => "NUMBER(3)"
    case _ => super.defaultSqlTypeName(tmd, size)
  }

  protected lazy val tableTableSpace = try {
    connectionConfig.map(_.getString("tableTableSpace"))
  } catch {
    case _: ConfigException.Missing => None
  }

  protected lazy val indexTableSpace = try {
    connectionConfig.map(_.getString("indexTableSpace"))
  } catch {
    case _: ConfigException.Missing => None
  }

  class ModelBuilder(mTables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext) extends JdbcModelBuilder(mTables, ignoreInvalidDefaults) {
    override def createColumnBuilder(tableBuilder: TableBuilder, meta: MColumn): ColumnBuilder = new ColumnBuilder(tableBuilder, meta) {
      final val OracleStringPattern = """^'(.*)' *$""".r
      override def default: Option[Option[Any]] = rawDefault.map {
        {
          case s: String if s.trim.equalsIgnoreCase("NULL") => None
          case v => Some((v, tpe) match {
            // Boolean (which are held in Chars) defaults sometimes have whitespace appended e.g. "1 ", so trim it
            case (v, "Char") if v.trim.length == 1 => v(0)
            case (OracleStringPattern(str), "String") => str
            case ("TRUE", "Char") => true
            case ("FALSE", "Char") => false
            case _ => super.default
          })
        }
      }
    }
  }

  override val scalarFrom: Option[String] = Some("dual")

  class QueryBuilder(tree: Node, state: CompilerState) extends super.QueryBuilder(tree, state) with RowNumberPagination {
    override protected val concatOperator = Some("||")

    override def expr(n: Node, skipParens: Boolean = false): Unit = {
      n match {
        case Library.NextValue(SequenceNode(name)) => b"`$name.nextval"
        case Library.CurrentValue(SequenceNode(name)) => b"`$name.currval"
        case RowNumber(by) =>
          b"row_number() over("
          if (by.isEmpty) b"order by (select 1 from dual)"
          else buildOrderByClause(by)
          b")"
        case Library.==(left: ProductNode, right: ProductNode) =>
          b"\("
          b"$left in ($right)"
          b"\)"
        case op @ Apply(sym: Library.SqlOperator, _) =>
          super.expr(op.nodeMapChildren {
            case ch @ Comprehension(_, _, _, orderBy, _, _, _) if orderBy.nonEmpty => {
              ch.copy(orderBy = Seq()) //sub-queries can't contain order by clauses
            }
            case x => x
          }, skipParens)
        case Library.Database() => b"sys_context('userenv','db_name')"
        // Oracle driver directly supports only getNumericFunctions()
        // "ABS,ACOS,ASIN,ATAN,ATAN2,CEILING,COS,EXP,FLOOR,LOG,LOG10,MOD,PI,POWER,ROUND,SIGN,SIN,SQRT,TAN,TRUNCATE";
        case Library.Degrees(ch) => b"57.2957795 * $ch"
        case Library.Radians(ch) => b"0.0174532925 * $ch"
        case Library.Repeat(ch, times) => b"ltrim(rpad('x', length($ch)*$times+1, $ch), 'x')"
        case _ => super.expr(n, skipParens)
      }
    }
  }

  class ColumnDDLBuilder(column: FieldSymbol) extends super.ColumnDDLBuilder(column) {
    override protected def appendOptions(sb: StringBuilder) {
      if (defaultLiteral ne null) sb append " DEFAULT " append defaultLiteral
      if (notNull) sb append " NOT NULL"
      if (primaryKey) sb append " PRIMARY KEY"
      // Don't do anything with autoincrement here. Create sequence and trigger on TableDDL
      if (jdbcType == columnTypes.booleanJdbcType)
        sb append " check (" append quoteIdentifier(column.name) append " in ('1', '0'))"
    }
  }

  class SequenceDDLBuilder[T](seq: Sequence[T]) extends super.SequenceDDLBuilder(seq) {
    override def buildDDL: DDL = {
      val b = new StringBuilder append "create sequence " append quoteIdentifier(seq.name)
      seq._increment.foreach {
        b append " increment by " append _
      }
      seq._minValue.foreach {
        b append " minvalue " append _
      }
      seq._maxValue.foreach {
        b append " maxvalue " append _
      }
      seq._start.foreach {
        b append " start with " append _
      }
      if (seq._cycle) {
        b append " cycle"
        //TODO Sue add nocache/cache size option
        val cacheSize = 20 // Oracle default http://www.dba-oracle.com/t_sequence_caching.htm
        for {
          maxValue <- seq._maxValue
          minValue <- seq._minValue
        } yield {
          try {
            val cycleSize = math.abs(maxValue.toString.toInt - minValue.toString.toInt)
            if (cacheSize > cycleSize) b append " cache " append cycleSize
          } catch {
            case _: Exception => //if max and min aren't convertible to ints, nothing to put here
          }
        }
      }
      DDL(b.toString, "drop sequence " + quoteIdentifier(seq.name))
    }
  }

  class TableDDLBuilder(table: Table[_]) extends super.TableDDLBuilder(table) {
    override protected def createPhase1 = Iterable(createTable) ++ primaryKeys.map(createPrimaryKey) ++ indexes.flatMap(createIndexStmts)

    override protected def createPhase2 = {
      val autoIncColumns = table.create_*.filter(c => c.options.contains(ColumnOption.AutoInc))
      val triggerStatements = autoIncColumns.flatMap { column =>
        val tableName = table.tableName
        val columnSequence = s"${tableName}_${column}autoinc"
        val columnName = column.name
        val autoincStatements = Seq("CREATE SEQUENCE " + columnSequence,
          """CREATE OR REPLACE TRIGGER """ + tableName + """_triggerid BEFORE INSERT ON """
            + quoteIdentifier(tableName) + """ FOR EACH ROW
            BEGIN
              SELECT """ + columnSequence + """.NEXTVAL
              into :NEW.""" + quoteIdentifier(columnName) + """
              FROM   dual;
            END;"""
        )
        autoincStatements
      }
      super.createPhase2 ++ triggerStatements
    }

    override protected def addForeignKey(fk: ForeignKey, sb: StringBuilder) {
      sb append "constraint " append quoteIdentifier(fk.name) append " foreign key("
      addForeignKeyColumnList(fk.linearizedSourceColumns, sb, tableNode.tableName)
      sb append ") references " append quoteTableName(fk.targetTable) append "("
      addForeignKeyColumnList(fk.linearizedTargetColumnsForOriginalTargetTable, sb, fk.targetTable.tableName)
      import model.ForeignKeyAction._
      sb append ") " append (fk.onUpdate match {
        case NoAction => "" //TODO Sue must be more
        case _ => "on update " + fk.onDelete.action
      })
      sb append (fk.onDelete match {
        case NoAction => "" //TODO Sue must be more
        case _ => " on delete " + fk.onDelete.action
      })
    }

    protected def createIndexStmts(idx: Index): Seq[String] = {
      val indexStmt = super.createIndex(idx) + indexTableSpace.map(t => s" tablespace $t").getOrElse("")
      /* Adding unique index does not imply a unique constraint and this is needed for any foreign keys,
       * so add constraint explicitly. Not enough to just add unique constraint. Doesn't always add an
       * index. */ //TODO Sue add test to show this
      if (idx.unique) {
        val sb = new StringBuilder
        sb append "ALTER TABLE " append quoteIdentifier(table.tableName) append " ADD "
        sb append "CONSTRAINT " append quoteIdentifier(idx.name + "_cons") append " UNIQUE("
        addIndexColumnList(idx.on, sb, idx.table.tableName)
        sb append ")"
        Seq(indexStmt, sb.toString())
      } else Seq(indexStmt)
    }

    override def createPrimaryKey(pk: PrimaryKey) = {
      super.createPrimaryKey(pk) + indexTableSpace.map(t => s" using index tablespace $t").getOrElse("")
    }
    override def createTable: String = {
      super.createTable + tableTableSpace.map(t => s" tablespace $t").getOrElse("")
    }
  }

  class SchemaActionExtensionMethodsImpl(schema: SchemaDescription) extends super.SchemaActionExtensionMethodsImpl(schema) {
    override def create: DriverAction[Unit, NoStream, Effect.Schema] = new SimpleJdbcDriverAction[Unit]("schema.create", schema.createStatements.toSeq) {
      def run(ctx: Backend#Context, sql: Iterable[String]): Unit =
        //need withStatement not withPreparedStatement because of synthetic autoinc with sequences
        for (s <- sql)
          ctx.session.withStatement()(stmt => stmt.execute(s))
    }
  }

  class DDLInvoker(ddl: DDL) extends super.DDLInvoker(ddl) {
    override def create(implicit session: Backend#Session): Unit = session.withTransaction {
      for (s <- ddl.createStatements)
        session.withStatement()(stmt => stmt.execute(s))
    }
  }

  class JdbcTypes extends super.JdbcTypes {
    override val booleanJdbcType = new BooleanJdbcType
    override val blobJdbcType = new BlobJdbcType

    /* Oracle does not have a proper BOOLEAN type. The suggested workaround is
     * CHAR with constants 1 and 0 for TRUE and FALSE. */
    class BooleanJdbcType extends super.BooleanJdbcType {
      override def sqlType = java.sql.Types.CHAR //ColumnDDLBuilder adds constraints to only be '1' or '0'
      override def valueToSQLLiteral(value: Boolean) = if (value) "1" else "0"
    }
    class BlobJdbcType extends super.BlobJdbcType {
      override def setValue(v: Blob, p: PreparedStatement, idx: Int) = {
        v match {
          case serialBlob: javax.sql.rowset.serial.SerialBlob => p.setBlob(idx, serialBlob.getBinaryStream)
          case _ => super.setValue(v, p, idx)
        }
      }
    }

    override val uuidJdbcType = new UUIDJdbcType {
      override def sqlType = java.sql.Types.BINARY

      override def sqlTypeName(size: Option[RelationalProfile.ColumnOption.Length]) = "RAW(16)"

      override def valueToSQLLiteral(value: UUID): String =
        "HEXTORAW('" + value.toString.replace("-", "") + "')"
    }

  }
  def connectionConfig: Option[Config] = None
}

object OracleProfile extends OracleProfile
