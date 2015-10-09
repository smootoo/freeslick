package freeslick

import com.typesafe.slick.testkit
import com.typesafe.slick.testkit.tests._
import com.typesafe.slick.testkit.util._
import org.junit.runner.RunWith
import slick.jdbc.meta.MTable
import slick.util.Logging

import scala.concurrent.{Await, ExecutionContext}

@RunWith(classOf[Testkit])
class MSSQLServer2008Test extends FreeslickDriverTest(MSSQLServerTest.testDB) {
  override def tests = {
    super.tests
      .filterNot(_ == classOf[testkit.tests.PlainSQLTest]) :+
      classOf[MSSQLServerPlainSQLTest]
    //Seq(classOf[FreeslickGroupByTest])
  }
}

object MSSQLServerTest extends Logging {
  lazy val testDB: TestDB = new ExternalJdbcTestDB("sqlserver2008") {
    val driver = MSSQLServerProfile
    import driver.api._

    override def localTables(implicit ec: ExecutionContext): DBIO[Vector[String]] =
      MTable.getTables(None, None, None, Some(Seq("TABLE"))).map(_.map(_.name.name))

    override def dropUserArtifacts(implicit session: profile.Backend#Session) =
      blockingRunOnSession { implicit ec =>
          for {
            tables <- localTables
            _ <- DBIO.seq((tables.map(t => sqlu"exec sp_MSdropconstraints #$t") ++
                           tables.map(t => sqlu"drop table #$t")): _*)
          } yield ()
        }


    override lazy val capabilities = driver.capabilities + TestDB.capabilities.plainSql
  }
}
