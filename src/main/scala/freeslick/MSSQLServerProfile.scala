package freeslick

import java.util.UUID

import slick.ast.TypeUtil.:@
import slick.dbio.DBIO
import slick.driver._
import slick.jdbc.meta.{ MColumn, MTable }
import slick.ast._
import slick.jdbc.{ JdbcModelBuilder, JdbcType, PositionedResult }
import slick.profile._
import slick.compiler._
import java.sql.{ ResultSet, Timestamp, Date, Time }

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext
import slick.ast._
import slick.util.MacroSupport.macroSupportInterpolation
import slick.profile.{ RelationalProfile, SqlProfile, Capability }
import slick.compiler.CompilerState
import slick.jdbc.{ JdbcModelBuilder, JdbcType }
import slick.jdbc.meta.{ MColumn, MTable }
import slick.model.Model

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
trait MSSQLServerProfile extends JdbcDriver { driver =>

  override protected def computeCapabilities: Set[Capability] = (super.computeCapabilities
    - JdbcProfile.capabilities.forceInsert
    - JdbcProfile.capabilities.returnInsertOther
    - JdbcProfile.capabilities.returnInsertKey //TODO Sue
    - SqlProfile.capabilities.sequence
    - JdbcProfile.capabilities.supportsByte
  )

  override protected def computeQueryCompiler =
    super.computeQueryCompiler.addAfter(Phase.rewriteBooleans, QueryCompiler.relationalPhases.last).
      addBefore(new ExistsToCount, QueryCompiler.relationalPhases.head)

  override val columnTypes = new JdbcTypes
  override def createModelBuilder(tables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext): JdbcModelBuilder =
    new ModelBuilder(tables, ignoreInvalidDefaults)
  override def createQueryBuilder(n: Node, state: CompilerState): QueryBuilder = new QueryBuilder(n, state)
  override def createColumnDDLBuilder(column: FieldSymbol, table: Table[_]): ColumnDDLBuilder = new ColumnDDLBuilder(column)
  override def createUpsertBuilder(node: Insert): InsertBuilder = new UpsertBuilder(node)
  override def defaultTables(implicit ec: ExecutionContext): DBIO[Seq[MTable]] =
    MTable.getTables(None, None, None, Some(Seq("TABLE")))

  override def defaultSqlTypeName(tmd: JdbcType[_], size: Option[RelationalProfile.ColumnOption.Length]): String = tmd.sqlType match {
    case java.sql.Types.BOOLEAN => "BIT"
    case java.sql.Types.BLOB => "IMAGE"
    case java.sql.Types.CLOB => "TEXT"
    case java.sql.Types.DOUBLE => "FLOAT(53)"
    case java.sql.Types.FLOAT => "FLOAT(24)"
    case _ => super.defaultSqlTypeName(tmd, size)
  }

  class ModelBuilder(mTables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext) extends JdbcModelBuilder(mTables, ignoreInvalidDefaults) {
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
        case (Some(t), None) => b"top $t "
        case (None, _) => if (c.orderBy.nonEmpty) b"top 100 percent "
      }
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
      case c @ LiteralNode(v) if c.volatileHint && jdbcTypeFor(c.tpe) == columnTypes.timeJdbcType =>
        b"cast("
        super.expr(n, skipParens)
        b" as ${columnTypes.timeJdbcType.sqlTypeName(None)})"
      case QueryParameter(extractor, tpe) if jdbcTypeFor(tpe) == columnTypes.timeJdbcType =>
        b"cast("
        super.expr(n, skipParens)
        b" as ${columnTypes.timeJdbcType.sqlTypeName(None)})"
      case Library.Substring(s, start) =>
        b"substring($s, ${QueryParameter.constOp[Int]("+")(_ + _)(start, LiteralNode(1))}, ${QueryParameter.constOp[Int]("+")(_ + _)(LiteralNode(999999998), LiteralNode(1))})\)"
      case Library.Repeat(s, count) =>
        b"replicate($s, $count)"
      case _ => super.expr(n, skipParens)
    }
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

  class JdbcTypes extends super.JdbcTypes {
    override val booleanJdbcType = new BooleanJdbcType
    override val byteJdbcType = new ByteJdbcType
    override val dateJdbcType = new DateJdbcType
    override val timeJdbcType = new TimeJdbcType
    override val timestampJdbcType = new TimestampJdbcType
    override val uuidJdbcType = new UUIDJdbcType {
      override def sqlTypeName(size: Option[RelationalProfile.ColumnOption.Length]) = "UNIQUEIDENTIFIER"
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
      override def valueToSQLLiteral(value: Time) = "(convert(time, {t '" + value + "'}))"
      override def getValue(r: ResultSet, idx: Int) = {
        val s = r.getString(idx)
        val sep = s.indexOf('.')
        if (sep == -1) Time.valueOf(s)
        else {
          val t = Time.valueOf(s.substring(0, sep))
          val millis = (("0." + s.substring(sep + 1)).toDouble * 1000.0).toInt
          t.setTime(t.getTime + millis)
          t
        }
      }
    }
    class TimestampJdbcType extends super.TimestampJdbcType {
      /* TIMESTAMP in SQL Server is a data type for sequence numbers. What we
       * want here is DATETIME. */
      override def sqlTypeName(size: Option[RelationalProfile.ColumnOption.Length]) = "DATETIME"
      override def valueToSQLLiteral(value: Timestamp) = "(convert(datetime, {ts '" + value + "'}))"
    }
    /* SQL Server's TINYINT is unsigned, so we use SMALLINT instead to store a signed byte value.
     * The JDBC driver also does not treat signed values correctly when reading bytes from result
     * sets, so we read as Short and then convert to Byte. */
    class ByteJdbcType extends super.ByteJdbcType {
      override def sqlTypeName(size: Option[RelationalProfile.ColumnOption.Length]) = "SMALLINT"
      //def setValue(v: Byte, p: PositionedParameters) = p.setByte(v)
      //def setOption(v: Option[Byte], p: PositionedParameters) = p.setByteOption(v)
      override def getValue(r: ResultSet, idx: Int) = r.getShort(idx).toByte
      //def updateValue(v: Byte, r: PositionedResult) = r.updateByte(v)
    }
  }
  /**
   * Query compiler phase that rewrites Exists calls in projections to
   * equivalent CountAll > 0 calls which can then be fused into aggregation
   * sub-queries in the fuseComprehensions phase.
   */
  class ExistsToCount extends Phase {
    val name = "mssql:existsToCount"

    def apply(state: CompilerState) = state.map(n => tr(n, false))

    protected def tr(n: Node, inSelect: Boolean): Node = n match {
      case b @ Bind(_, _, sel) => b.nodeMapChildren { n => tr(n, n eq sel) }
      case f: FilteredQuery => f.nodeMapChildren(tr(_, false))
      case a @ Library.Exists(ch) if inSelect =>
        Library.>.typed[Boolean](Library.CountAll.typed[Int](tr(ch, true)), LiteralNode(0))
      case n => n.nodeMapChildren(ch => tr(ch, inSelect))
    }
  }
}

object MSSQLServerProfile extends MSSQLServerProfile
