package slick.driver.test

import com.typesafe.slick.testkit
import freeslick.MSSQLProfile
import org.junit.runner.RunWith
import com.typesafe.slick.testkit.util._
import slick.dbio._
import slick.jdbc.meta.{ MQName, MForeignKey, MTable }
import slick.jdbc.{ StaticQuery => Q, ResultSetAction }
import slick.util.Logging

import scala.concurrent.ExecutionContext

@RunWith(classOf[Testkit])
class SQLServer2008Test extends DriverTest(SQLServerTest.SQLServer2008Test) {
  override def tests = {
    super.tests
      //TODO Sue SQL Server specific ModelBuilderTest
      .filterNot(_ == classOf[testkit.tests.ModelBuilderTest])
      .filterNot(_ == classOf[testkit.tests.PlainSQLTest]) /* TODO Sue :+
      classOf[SQLServerPlainSQLTest] */
  }
}

object SQLServerTest extends Logging {
  lazy val SQLServer2008Test: TestDB = new ExternalJdbcTestDB("sqlserver2008") {
    val driver = MSSQLProfile

    override def localTables(implicit ec: ExecutionContext): DBIO[Vector[String]] =
      MTable.getTables(None, None, None, Some(Seq("TABLE"))).map(x => x.map(y => y.name.name))

    override def dropUserArtifacts(implicit session: profile.Backend#Session) = {
      val localTables = getLocalTables
      localTables.foreach(t => (Q.u + s"exec sp_MSdropconstraints $t").execute)
      localTables.foreach(t => (Q.u + s"DROP TABLE $t").execute)
    }

    override lazy val capabilities = driver.capabilities + TestDB.capabilities.plainSql
  }

  //  import MSSQLProfile.api._
  //  def SQLServerVersion(tdb: TestDB): Option[String] =
  //    tdb.createDB().run(SimpleDBIO { implicit s =>
  //      sql"select @@version".as[(String)]
  //    })
  //
  //  def SQLServer2000Version(tdb: TestDB) = SQLServerVersion(tdb: TestDB).exists(_.contains("SQL Server  2000"))
  //
  //  def SQLServer2005Version(tdb: TestDB) = SQLServerVersion(tdb: TestDB).exists(_.contains("SQL Server 2005"))
}
