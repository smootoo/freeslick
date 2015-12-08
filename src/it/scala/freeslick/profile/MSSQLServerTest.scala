package freeslick.profile

import com.typesafe.slick.testkit
import com.typesafe.slick.testkit.tests._
import com.typesafe.slick.testkit.util._
import freeslick.MSSQLServerProfile
import freeslick.testkit.{UUIDTest, FreeslickGroupByTest, MSSQLServerPlainSQLTest}
import org.junit.runner.RunWith
import slick.dbio._
import slick.jdbc.meta.MTable
import slick.util.Logging

import scala.concurrent.{Await, ExecutionContext}

@RunWith(classOf[Testkit])
class MSSQLServer2008Test extends FreeslickDriverTest(MSSQLServerTest.MSSQLServerTestDB("sqlserver2008")) {
  override def tests = {
    super.tests
      .filterNot(_ == classOf[testkit.tests.PlainSQLTest]) :+
      classOf[MSSQLServerPlainSQLTest]
    //Seq(classOf[UUIDTest])
  }
}

@RunWith(classOf[Testkit])
class MSSQLServer2012Test extends FreeslickDriverTest(MSSQLServerTest.MSSQLServerTestDB("sqlserver2012")) {
  override def tests = {
    super.tests
      .filterNot(_ == classOf[testkit.tests.PlainSQLTest]) :+
      classOf[MSSQLServerPlainSQLTest]
  }
}

@RunWith(classOf[Testkit])
class MSSQLServer2014Test extends FreeslickDriverTest(MSSQLServerTest.MSSQLServerTestDB("sqlserver2014")) {
  override def tests = {
    super.tests
      .filterNot(_ == classOf[testkit.tests.PlainSQLTest]) :+
      classOf[MSSQLServerPlainSQLTest]
  }
}

object MSSQLServerTest extends Logging {
  def MSSQLServerTestDB(testDBName: String): TestDB = new ExternalJdbcTestDB(testDBName) {
    val driver = MSSQLServerProfile
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
        DBIO.seq(sql"select @@VERSION, SERVERPROPERTY('productversion'), SERVERPROPERTY('productlevel'), SERVERPROPERTY('edition')".
          as[(String, String, String, String)].map(x => println(s"[DBConnection information $x]"))
        )))
    }


    override lazy val capabilities = driver.capabilities + TestDB.capabilities.plainSql
  }
}
