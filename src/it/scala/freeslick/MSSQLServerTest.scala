package freeslick

import com.typesafe.slick.testkit
import com.typesafe.slick.testkit.util._
import org.junit.runner.RunWith
import slick.dbio._
import slick.jdbc.meta.MTable
import slick.jdbc.{StaticQuery => Q}
import slick.util.Logging

import scala.concurrent.ExecutionContext

@RunWith(classOf[Testkit])
class MSSQLServer2008Test extends DriverTest(MSSQLServerTest.MSSQLServer2008Test) {
  override def tests = {
    super.tests
      .filterNot(_ == classOf[testkit.tests.ModelBuilderTest])
      .filterNot(_ == classOf[testkit.tests.PlainSQLTest]) :+
      classOf[MSSQLServerModelBuilderTest] :+
      classOf[MSSQLServerPlainSQLTest]
  }
}

object MSSQLServerTest extends Logging {
  lazy val MSSQLServer2008Test: TestDB = new ExternalJdbcTestDB("sqlserver2008") {
    val driver = MSSQLServerProfile

    override def localTables(implicit ec: ExecutionContext): DBIO[Vector[String]] =
      MTable.getTables(None, None, None, Some(Seq("TABLE"))).map(_.map(_.name.name))

    override def dropUserArtifacts(implicit session: profile.Backend#Session) = {
      val localTables = getLocalTables
      localTables.foreach(t => (Q.u + s"exec sp_MSdropconstraints $t").execute)
      localTables.foreach(t => (Q.u + s"DROP TABLE $t").execute)
    }

    override lazy val capabilities = driver.capabilities + TestDB.capabilities.plainSql
  }
}
