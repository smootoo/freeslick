package freeslick

import com.typesafe.slick.testkit
import com.typesafe.slick.testkit.tests.SequenceTest
import com.typesafe.slick.testkit.util._
import org.junit.runner.RunWith
import slick.dbio._
import slick.jdbc.meta.MTable
import slick.jdbc.{StaticQuery => Q}
import slick.profile.SqlProfile
import slick.util.Logging

import scala.concurrent.ExecutionContext

@RunWith(classOf[Testkit])
class OracleITTest extends FreeslickDriverTest(OracleTest.Oracle11gTest) {
  override def tests = {
    super.tests
      .filterNot(_ == classOf[testkit.tests.JoinTest]) // Replaced with a FreeslickJoinTest
  }
}

object OracleTest extends Logging {
  lazy val Oracle11gTest: TestDB = new ExternalJdbcTestDB("oracle11g") {
    val driver = new OracleProfile {
      override def connectionConfig = Some(config.getConfig("adminConn"))
      override def toString() = "OracleDriverTest" // ModelBuilderTest looks for a classname with OracleDriver in
    }
    import driver.api._

    override def localTables(implicit ec: ExecutionContext): DBIO[Vector[String]] = {
      val tableNames = driver.defaultTables.map(_.map(_.name.name)).map(_.toVector)
      tableNames
    }


    def localSequences(implicit ec: ExecutionContext): DBIO[Vector[String]] = {
      // user_sequences much quicker than going to meta if you don't know the schema they are going to be in
      sql"select sequence_Name from user_sequences".as[String]
    }

    override def getLocalSequences(implicit session: profile.Backend#Session): List[String] = {
      blockingRunOnSession(ec => localSequences(ec)).toList
    }

    override def dropUserArtifacts(implicit session: profile.Backend#Session) = {
      val localTables = getLocalTables
      localTables.foreach(t => (Q.u + s"drop table " + driver.quoteIdentifier(t) + " cascade constraints").execute)
      val localSequences = getLocalSequences
      localSequences.foreach(s => (Q.u + "drop sequence " + driver.quoteIdentifier(s)).execute)
    }

    override lazy val capabilities = driver.capabilities + TestDB.capabilities.plainSql
  }
}
