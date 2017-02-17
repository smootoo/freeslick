package freeslick

import java.sql.{ Date, ResultSet, Time, Timestamp }
import java.util.UUID

import freeslick.profile.utils.{ DriverRowNumberPagination, FreeslickRewriteBooleans }
import slick.ast._
import slick.compiler.{ CompilerState, _ }
import slick.dbio.DBIO
import slick.driver._
import slick.jdbc.meta.{ MColumn, MTable }
import slick.jdbc.{ JdbcModelBuilder, JdbcType }
import slick.profile.{ Capability, SqlProfile }
import slick.util.ConstArray
import slick.util.MacroSupport.macroSupportInterpolation

import scala.concurrent.ExecutionContext

/**
 * Slick profile for Microsoft SQL Server.
 *
 * This profile implements the `scala.slick.driver.JdbcProfile`
 * ''without'' the following capabilities:
 *
 * <ul>
 *   <li>`scala.slick.driver.JdbcProfile.capabilities.returnInsertOther`:
 *     When returning columns from an INSERT operation, only a single column
 *     may be specified which must be the table's AutoInc column.</li>
 *   <li>`scala.slick.profile.SqlProfile.capabilities.sequence`:
 *     Sequences are not supported because SQLServer does not have this
 *     feature.</li>
 *   <li>`scala.slick.driver.JdbcProfile.capabilities.forceInsert`:
 *     Inserting explicit values into AutoInc columns with ''forceInsert''
 *     operations is not supported.</li>
 * </ul>
 */
trait MSSQLServerProfile extends JdbcDriver with DriverRowNumberPagination { driver =>

  override protected def computeCapabilities: Set[Capability] = (super.computeCapabilities
    - JdbcProfile.capabilities.forceInsert
    - JdbcProfile.capabilities.returnInsertOther
    - SqlProfile.capabilities.sequence
    - JdbcProfile.capabilities.supportsByte
  )

  // "merge into" (i.e. server side upsert) won't return generated keys in sqlserver jdbc
  // on an update, it seems to return the value for the last insert
  // this will do a select then insert or update in a transaction. The insert will
  // return generated keys
  override protected lazy val useServerSideUpsertReturning = false

  override protected def computeQueryCompiler =
    super.computeQueryCompiler.addAfter(new FreeslickRewriteBooleans, QueryCompiler.sqlPhases.last)

  override val columnTypes = new JdbcTypes
  override def createModelBuilder(tables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext): JdbcModelBuilder =
    new MSSQLModelBuilder(tables, ignoreInvalidDefaults)
  override def createQueryBuilder(n: Node, state: CompilerState): QueryBuilder = new QueryBuilder(n, state)
  override def createColumnDDLBuilder(column: FieldSymbol, table: Table[_]): ColumnDDLBuilder = new ColumnDDLBuilder(column)
  override def createUpsertBuilder(node: Insert): super.InsertBuilder = new UpsertBuilder(node)
  override def createInsertBuilder(node: Insert): super.InsertBuilder = new InsertBuilder(node)

  override def defaultTables(implicit ec: ExecutionContext): DBIO[Seq[MTable]] =
    MTable.getTables(None, Some("dbo"), None, Some(Seq("TABLE")))

  override def defaultSqlTypeName(tmd: JdbcType[_], sym: Option[FieldSymbol]): String = tmd.sqlType match {
    case java.sql.Types.BOOLEAN => "BIT"
    case java.sql.Types.BLOB => "IMAGE"
    case java.sql.Types.CLOB => "TEXT"
    case java.sql.Types.DOUBLE => "FLOAT(53)"
    case java.sql.Types.FLOAT => "FLOAT(24)"
    case _ => super.defaultSqlTypeName(tmd, sym)
  }

  class MSSQLModelBuilder(mTables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext) extends JdbcModelBuilder(mTables, ignoreInvalidDefaults) {
    override def readColumns(t: MTable): DBIO[Vector[MColumn]] = {
      val cols = t.getColumns.map(_.sortBy(_.ordinalPosition))
      cols.map(_.map { mc =>
        {
          mc.sqlType match {
            // bug in the jtds driver http://sourceforge.net/p/jtds/bugs/679/
            case 12 if mc.name.endsWith("java_sql_Date") => mc.copy(sqlType = java.sql.Types.DATE)
            case 12 if mc.name.endsWith("java_sql_Time") => mc.copy(sqlType = java.sql.Types.TIME)
            case _ => mc
          }
        }
      })
    }

    override def createColumnBuilder(tableBuilder: TableBuilder, meta: MColumn): ColumnBuilder = new ColumnBuilder(tableBuilder, meta) {
      //TODO Sue why end up with all these brackets?
      override def rawDefault = super.rawDefault.map(s => s.stripPrefix("(").stripPrefix("(").stripSuffix(")").stripSuffix(")"))
      override def default = rawDefault.map((_, tpe)).collect {
        case (v, "Boolean") if v != "NULL" => {
          Some(v match {
            case "0" => Some(false)
            case _ => Some(true)
          })
        }
      }.getOrElse { super.default }
    }
  }

  class QueryBuilder(tree: Node, state: CompilerState) extends super.QueryBuilder(tree, state) with RowNumberPagination {
    override protected val supportsTuples = false
    override protected val concatOperator = Some("+")

    override protected def buildSelectModifiers(c: Comprehension) {
      (c.fetch, c.offset) match {
        case (Some(t), Some(d)) => b"top ($d + $t) "
        case (Some(t), None) => b"top ($t) "
        case (None, _) => if (c.orderBy.nonEmpty) b"top 100 percent "
      }
      super.buildSelectModifiers(c)
    }

    override protected def buildOrdering(n: Node, o: Ordering) {
      if (o.nulls.last && !o.direction.desc)
        b"case when ($n) is null then 1 else 0 end,"
      else if (o.nulls.first && o.direction.desc)
        b"case when ($n) is null then 0 else 1 end,"
      expr(n)
      if (o.direction.desc) b" desc"
    }

    override def expr(n: Node, skipParens: Boolean = false): Unit = n match {
      // Cast bind variables of type TIME to TIME (otherwise they're treated as TIMESTAMP)
      case c @ LiteralNode(v) if c.volatileHint && jdbcTypeFor(c.nodeType) == columnTypes.timeJdbcType =>
        b"cast("
        super.expr(n, skipParens)
        b" as ${columnTypes.timeJdbcType.sqlTypeName(None)})"
      case QueryParameter(extractor, tpe, id) if jdbcTypeFor(tpe) == columnTypes.timeJdbcType =>
        b"cast("
        super.expr(n, skipParens)
        b" as ${columnTypes.timeJdbcType.sqlTypeName(None)})"
      case Library.Substring(s, start) =>
        b"substring($s, ${QueryParameter.constOp[Int]("+")(_ + _)(start, LiteralNode(1).infer())}, ${QueryParameter.constOp[Int]("+")(_ + _)(LiteralNode(999999998).infer(), LiteralNode(1).infer())})\)"
      case Library.Repeat(s, count) =>
        b"replicate($s, $count)"
      case _ => super.expr(n, skipParens)
    }

    override protected def buildGroupByClause(groupBy: Option[Node]) = building(OtherPart)(
      groupBy.foreach { n =>
        val children = n match {
          case ProductNode(es) => es
          case e => ConstArray(e)
        }
        // SQLServer can't have explicit literals in Group By http://www.sql-server-helper.com/error-messages/msg-164.aspx
        def literalChild(node: Node): Boolean =
          node match {
            case l @ LiteralNode(_) if !l.volatileHint => true
            case Apply(_, ch) => ch.forall(literalChild)
            case _ => false
          }
        val nonLiteralChildren = children.filter(x => !literalChild(x))
        if (nonLiteralChildren.nonEmpty) {
          b"\ngroup by "
          b.sep(nonLiteralChildren, ", ")(buildGroupByColumn)
        }
      })

  }

  class ColumnDDLBuilder(column: FieldSymbol) extends super.ColumnDDLBuilder(column) {
    override protected def appendOptions(sb: StringBuilder) {
      if (defaultLiteral ne null) sb append " DEFAULT " append defaultLiteral
      if (notNull) sb append " NOT NULL"
      if (primaryKey) sb append " PRIMARY KEY"
      if (autoIncrement) sb append " IDENTITY"
    }
  }

  class UpsertBuilder(ins: Insert) extends super.UpsertBuilder(ins) {
    // SQL Server need statement terminated with ;
    override protected def buildMergeEnd: String = super.buildMergeEnd + ";"
    //holdlock needed https://www.mssqltips.com/sqlservertip/3074/use-caution-with-sql-servers-merge-statement/
    override protected def buildMergeStart: String = s"merge into $tableName with (holdlock) t using ("
  }

  class InsertBuilder(ins: Insert) extends super.InsertBuilder(ins) {
    override protected def emptyInsert: String = s"insert into $tableName default values"
  }

  class JdbcTypes extends super.JdbcTypes {
    override val booleanJdbcType = new BooleanJdbcType
    override val byteJdbcType = new ByteJdbcType
    override val dateJdbcType = new DateJdbcType
    override val timeJdbcType = new TimeJdbcType
    override val timestampJdbcType = new TimestampJdbcType
    override val uuidJdbcType = new UUIDJdbcType {
      override def sqlTypeName(sym: Option[FieldSymbol]) = "UNIQUEIDENTIFIER"
      override def valueToSQLLiteral(uuid: UUID) = s"'${uuid.toString}'"
    }
    /* SQL Server does not have a proper BOOLEAN type. The suggested workaround is
     * BIT with constants 1 and 0 for TRUE and FALSE. */
    class BooleanJdbcType extends super.BooleanJdbcType {
      override def valueToSQLLiteral(value: Boolean) = if (value) "1" else "0"
    }
    /* Selecting a straight Date or Timestamp literal fails with a NPE (probably
     * because the type information gets lost along the way), so we cast all Date
     * and Timestamp values to the proper type. This work-around does not seem to
     * be required for Time values. */
    class DateJdbcType extends super.DateJdbcType {
      override def valueToSQLLiteral(value: Date) = "(convert(date, {d '" + value + "'}))"
    }
    class TimeJdbcType extends super.TimeJdbcType {
      lazy val timeRe = """^.*(\d\d:\d\d:\d\d).(\d*)$""".r
      override def valueToSQLLiteral(value: Time) = "(convert(time, {t '" + value + "'}))"
      override def getValue(r: ResultSet, idx: Int) = {
        val s = r.getString(idx)
        s match {
          case timeRe(timePart, milliPart) =>
            val t = Time.valueOf(timePart)
            val millis = (("0." + milliPart).toDouble * 1000).toInt
            t.setTime(t.getTime + millis)
            t
          case _ => Time.valueOf(s)
        }

      }
    }
    class TimestampJdbcType extends super.TimestampJdbcType {
      /* TIMESTAMP in SQL Server is a data type for sequence numbers. What we
       * want here is DATETIME. */
      override def sqlTypeName(sym: Option[FieldSymbol]) = "DATETIME"
      override def valueToSQLLiteral(value: Timestamp) = "(convert(datetime, {ts '" + value + "'}))"
    }
    /* SQL Server's TINYINT is unsigned, so we use SMALLINT instead to store a signed byte value.
     * The JDBC driver also does not treat signed values correctly when reading bytes from result
     * sets, so we read as Short and then convert to Byte. */
    class ByteJdbcType extends super.ByteJdbcType {
      override def sqlTypeName(sym: Option[FieldSymbol]) = "SMALLINT"
      //def setValue(v: Byte, p: PositionedParameters) = p.setByte(v)
      //def setOption(v: Option[Byte], p: PositionedParameters) = p.setByteOption(v)
      override def getValue(r: ResultSet, idx: Int) = r.getShort(idx).toByte
      //def updateValue(v: Byte, r: PositionedResult) = r.updateByte(v)
    }
  }
}

object MSSQLServerProfile extends MSSQLServerProfile

