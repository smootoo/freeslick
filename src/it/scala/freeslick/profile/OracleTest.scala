package freeslick.profile

import com.typesafe.slick.testkit
import com.typesafe.slick.testkit.tests._
import com.typesafe.slick.testkit.util._
import freeslick.OracleProfile
import freeslick.testkit._
import org.junit.runner.RunWith
import slick.util.Logging

import scala.concurrent.ExecutionContext

@RunWith(classOf[Testkit])
class OracleITTest extends FreeslickDriverTest(OracleTest.Oracle11gTest("oracle11g")) {
  override def tests = {
    super.tests
      .filterNot(_ == classOf[testkit.tests.JoinTest]) // Replaced with a FreeslickJoinTest
    //Seq(classOf[ForeignKeyTest], classOf[UniqueIndexFKTest])
  }
}

@RunWith(classOf[Testkit])
class OracleNoTSITTest extends FreeslickDriverTest(OracleTest.Oracle11gTest("oracle11gNoTableSpace")) {
  override def tests = {
    Seq(classOf[testkit.tests.ModelBuilderTest]) // testing syntax if no tablespaces specified
  }
}

object OracleTest extends Logging {
  def Oracle11gTest(testDBName: String): TestDB = new ExternalJdbcTestDB(testDBName) {
    val driver = new OracleProfile {
      override def connectionConfig = Some(config.getConfig("adminConn"))
      override def toString() = "OracleDriverTest" // ModelBuilderTest looks for a classname with OracleDriver in
    }
    import driver.api._

    override def localTables(implicit ec: ExecutionContext): DBIO[Vector[String]] = {
      val tableNames = driver.defaultTables.map(_.map(_.name.name)).map(_.toVector)
      tableNames
    }


    override def localSequences(implicit ec: ExecutionContext): DBIO[Vector[String]] = {
      // user_sequences much quicker than going to meta if you don't know the schema they are going to be in
      sql"select sequence_Name from user_sequences".as[String]
    }

    override def dropUserArtifacts(implicit session: profile.Backend#Session) =
      blockingRunOnSession { implicit ec =>
           for {
             tables <- localTables
             sequences <- localSequences
             _ <- DBIO.seq(tables.map(t => sqlu"drop table #${driver.quoteIdentifier(t)} cascade constraints") ++
                            sequences.map(s => sqlu"drop sequence #${driver.quoteIdentifier(s)}"): _*)
           } yield ()
         }

    override lazy val capabilities = driver.capabilities + TestDB.capabilities.plainSql
  }
}
