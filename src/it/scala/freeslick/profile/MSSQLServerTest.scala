package freeslick.profile

import com.typesafe.slick.testkit
import com.typesafe.slick.testkit.tests._
import com.typesafe.slick.testkit.util._
import freeslick.{MSSQLServerProfile, MSJDBCSQLServerProfile}
import freeslick.testkit.{UUIDTest, FreeslickGroupByTest, MSSQLServerPlainSQLTest}
import org.junit.runner.RunWith
import slick.dbio._
import slick.driver.{JdbcDriver, JdbcProfile}
import slick.jdbc.meta.MTable
import slick.util.Logging

import scala.concurrent.{Await, ExecutionContext}

@RunWith(classOf[Testkit])
class MSSQLServer2008Test extends FreeslickDriverTest(MSSQLServerTest.MSSQLServerTestDB("sqlserver2008jtds")) {
  override def tests = {
    super.tests
      .filterNot(_ == classOf[testkit.tests.PlainSQLTest]) :+
      classOf[MSSQLServerPlainSQLTest]
    //Seq(classOf[UUIDTest])
  }
}

@RunWith(classOf[Testkit])
class MSSQLServer2012Test extends FreeslickDriverTest(MSSQLServerTest.MSSQLServerTestDB("sqlserver2012jtds")) {
  override def tests = {
    super.tests
      .filterNot(_ == classOf[testkit.tests.PlainSQLTest]) :+
      classOf[MSSQLServerPlainSQLTest]
  }
}

@RunWith(classOf[Testkit])
class MSSQLServer2014Test extends FreeslickDriverTest(MSSQLServerTest.MSSQLServerTestDB("sqlserver2014jtds")) {
  override def tests = {
    super.tests
      .filterNot(_ == classOf[testkit.tests.PlainSQLTest]) :+
      classOf[MSSQLServerPlainSQLTest]
  }
}

@RunWith(classOf[Testkit])
class MSSQLServer2008MSJDBCTest extends
  FreeslickDriverTest(MSSQLServerTest.MSSQLServerTestDB("sqlserver2008msjdbc", MSJDBCSQLServerProfile))

@RunWith(classOf[Testkit])
class MSSQLServer2012MSJDBCTest extends
  FreeslickDriverTest(MSSQLServerTest.MSSQLServerTestDB("sqlserver2010msjdbc", MSJDBCSQLServerProfile))

@RunWith(classOf[Testkit])
class MSSQLServer2014MSJDBCTest extends
  FreeslickDriverTest(MSSQLServerTest.MSSQLServerTestDB("sqlserver2014msjdbc", MSJDBCSQLServerProfile))

object MSSQLServerTest extends Logging {
  def MSSQLServerTestDB(testDBName: String,
                        testDriver: JdbcDriver = MSSQLServerProfile): TestDB =
    new ExternalJdbcTestDB(testDBName) {
      override val driver: JdbcDriver = testDriver
      import driver.api._

      override def localTables(implicit ec: ExecutionContext): DBIO[Vector[String]] =
        MTable.getTables(None, Some("dbo"), None, Some(Seq("TABLE"))).map(_.map(_.name.name))

      override def dropUserArtifacts(implicit session: profile.Backend#Session) =
        blockingRunOnSession { implicit ec =>
          for {
            tables <- localTables
            _ <- DBIO.seq(tables.map(t => sqlu"exec sp_MSdropconstraints #$t") ++
              tables.map(t => sqlu"drop table #$t"): _*)
          } yield ()
        }

      override def cleanUpBefore() {
        super.cleanUpBefore()
        import scala.concurrent.ExecutionContext.Implicits.global
        await(databaseFor("adminConn").run(
          DBIO.seq(sql"""select @@VERSION,
        CAST(SERVERPROPERTY('ProductVersion') AS NVARCHAR(128)), 
        CAST(SERVERPROPERTY('ProductLevel') AS NVARCHAR(128)), 
        CAST(SERVERPROPERTY('edition') AS NVARCHAR(128))""".
            as[(String, String, String, String)].map(x => println(s"[DBConnection information $x]"))
          )))
      }
      override lazy val capabilities = driver.capabilities + TestDB.capabilities.plainSql
    }
}
