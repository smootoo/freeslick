package scala.slick.driver.test

import com.typesafe.slick.testkit
import freeslick.MSSQLProfile
import org.junit.runner.RunWith
import com.typesafe.slick.testkit.util._
import scala.slick.jdbc.meta.MTable
import scala.slick.jdbc.{ StaticQuery => Q }
import scala.slick.jdbc.StaticQuery.interpolation
import scala.slick.jdbc.GetResult._

@RunWith(classOf[Testkit])
class SQLServer2000Test extends DriverTest(SQLServerTest.tdb("sqlserver2000")) {
  override def tests = SQLServerTest.filterTests(tdb, super.tests)
}

@RunWith(classOf[Testkit])
class SQLServer2005Test extends DriverTest(SQLServerTest.tdb("sqlserver2005")) {
  override def tests = SQLServerTest.filterTests(tdb, super.tests)
}

@RunWith(classOf[Testkit])
class SQLServer2008Test extends DriverTest(SQLServerTest.tdb("sqlserver2008")) {
  override def tests = SQLServerTest.filterTests(tdb, super.tests)
}

object SQLServerTest {
  def tdb(confName: String) = new ExternalJdbcTestDB(confName) {
    type Driver = MSSQLProfile.type
    val driver = MSSQLProfile

    override def getLocalTables(implicit session: profile.Backend#Session) = {
      driver.getTables.mapResult(_.name.name).list
    }

    override def dropUserArtifacts(implicit session: profile.Backend#Session) = {

      for (tableName <- getLocalTables) {
        MTable.getTables(None, None, Some(tableName), None).firstOption.foreach {
          metaData =>
            // No cascade delete, so find all the foreign keys and drop them first
            metaData.getExportedKeys.foreach { key =>
              val fkTable = key.fkTable.name
              val fkName = key.fkName
              fkName.foreach(c => Q.updateNA(s"ALTER TABLE $fkTable DROP CONSTRAINT $c").execute())
            }
            Q.updateNA(s"DROP TABLE $tableName").execute()
        }
      }
    }

    override lazy val capabilities =
      driver.capabilities + TestDB.plainSql + TestDB.plainSqlWide
  }
  import scala.slick.driver.JdbcDriver.backend.Database.dynamicSession

  def SQLServerVersion(tdb: TestDB) = tdb.createDB().withDynSession {
    sql"select @@version".as[(String)].list.headOption
  }
  def SQLServer2000Version(tdb: TestDB) = SQLServerVersion(tdb: TestDB).exists(_.contains("SQL Server  2000"))
  def SQLServer2005Version(tdb: TestDB) = SQLServerVersion(tdb: TestDB).exists(_.contains("SQL Server 2005"))

  def filterTests(tdb: TestDB, tests: List[Class[_ <: TestkitTest[_ >: Null <: TestDB]]]) =
    tests.filterNot((SQLServer2000Version(tdb: TestDB) || SQLServer2005Version(tdb: TestDB)) && List(
      // These tests don't work on SQLServer 2000 or 2005. Usually row_number or date functions.
      // I will work on the date ones and fix those
      classOf[testkit.tests.CountTest],
      classOf[testkit.tests.JdbcTypeTest],
      classOf[testkit.tests.JoinTest],
      classOf[testkit.tests.MetaModelTest],
      classOf[testkit.tests.NewQuerySemanticsTest],
      classOf[testkit.tests.PagingTest],
      classOf[testkit.tests.JdbcScalarFunctionTest]
    ).contains(_))
}
