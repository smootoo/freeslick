package freeslick

import java.sql._
import java.util.UUID

import freeslick.profile.utils.{ DriverRowNumberPagination, FreeslickRewriteBooleans }
import slick.ast._
import slick.compiler.{ CompilerState, _ }
import slick.dbio.DBIO
import slick.driver._
import slick.jdbc.meta.{ DatabaseMeta, MQName, MColumn, MTable }
import slick.jdbc.{ PositionedResult, ResultSetAction, JdbcModelBuilder, JdbcType }
import slick.profile.{ Capability, SqlProfile }
import slick.util.ConstArray
import slick.util.MacroSupport.macroSupportInterpolation

import scala.concurrent.ExecutionContext

/**
 * Slick profile for Microsoft SQL Server with MS JDBC Driver.
 * Extension of the freeslick standard MSSQL profile, but with
 * support for the Microsoft JDBC driver
 *
 */
trait MSJDBCSQLServerProfile extends MSSQLServerProfile { driver =>

  override def createModelBuilder(tables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext): JdbcModelBuilder =
    new MSJDBCModelBuilder(tables, ignoreInvalidDefaults)
  class MSJDBCModelBuilder(mTables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext)
      extends MSSQLModelBuilder(mTables, ignoreInvalidDefaults) {
    override def readColumns(t: MTable): DBIO[Vector[MColumn]] = {
      // this 2 helper functions are private in slick, so copied here
      def optionalFrom(r: PositionedResult) = {
        val cat = r.nextStringOption
        val schema = r.nextStringOption
        r.nextStringOption map (MQName(cat, schema, _))
      }
      def yesNoOpt(r: PositionedResult) = if (r.hasMoreColumns) r.nextString match {
        case "YES" => Some(true)
        case "NO" => Some(false)
        case _ => None
      }
      else None
      // This is lifted from slick's MColumn.getColumns function
      // MS jdbc driver doesn't return the sourceDataType field in the resultset
      ResultSetAction[MColumn](
        _.metaData.getColumns(t.name.catalog_?, t.name.schema_?, t.name.name, "%")) { r =>
          MColumn(MQName(r.<<, r.<<, r.<<), r.<<, r.<<, r.<<, r.<<, r.skip.<<, r.<<, r.nextInt() match {
            case DatabaseMetaData.columnNoNulls => Some(false)
            case DatabaseMetaData.columnNullable => Some(true)
            case _ => None
          }, r.<<, r.<<, r.skip.skip.<<, r.<<, yesNoOpt(r),
            if (r.hasMoreColumns) optionalFrom(r) else None,
            None, // MS jdbc driver missing this value in resultset, so skip it
            if (r.hasMoreColumns) yesNoOpt(r) else None)
        }.map(_.sortBy(_.ordinalPosition))
    }
  }
}

object MSJDBCSQLServerProfile extends MSJDBCSQLServerProfile

