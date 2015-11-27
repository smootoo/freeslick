package freeslick.profile

import com.typesafe.slick.testkit
import com.typesafe.slick.testkit.tests._
import com.typesafe.slick.testkit.util._
import freeslick.DB2Profile
import freeslick.testkit._
import org.junit.runner.RunWith
import slick.jdbc.meta.MTable
import slick.util.Logging

import scala.concurrent.ExecutionContext

@RunWith(classOf[Testkit])
class DB2ITTest extends FreeslickDriverTest(DB2Test.DB2Test("db2")) {
  override def tests = {
    super.tests
      .filterNot(_ == classOf[testkit.tests.JoinTest]) // Replaced with a FreeslickJoinTest
      .filterNot(_ == classOf[testkit.tests.MutateTest]) // DB2 has restrictions on mutable RowSets see FreeslickMutateTest
    //Seq(classOf[UUIDTest])
  }
}

@RunWith(classOf[Testkit])
class DB2NoTSITTest extends FreeslickDriverTest(DB2Test.DB2Test("db2NoTableSpace")) {
  override def tests = {
    Seq(classOf[testkit.tests.ModelBuilderTest]) // testing syntax if no tablespaces specified
  }
}

object DB2Test extends Logging {
  def DB2Test(testDBName: String): TestDB = new ExternalJdbcTestDB(testDBName) {
    val driver = new DB2Profile {
      override def connectionConfig = Some(config.getConfig("adminConn"))
    }
    import driver.api._

    def adminSchema: String = config.getConfig("adminConn").getString("user").toUpperCase
    def mTableIdentifier(mTable: MTable): String =
      mTable.name.schema.map(driver.quoteIdentifier(_) + ".").getOrElse("") + driver.quoteIdentifier(mTable.name.name)
    override def localTables(implicit ec: ExecutionContext): DBIO[Vector[String]] = {
      MTable.getTables(None, Some(adminSchema), None, Some(Seq("TABLE"))).map(_.map(mq => mTableIdentifier(mq)))
    }

    override def localSequences(implicit ec: ExecutionContext): DBIO[Vector[String]] = {
      sql"select sequence_schema, sequence_name from user_sequences".as[(String, String)].map(_.map{case(schema, name) =>
        s"${driver.quoteIdentifier(schema)}.${driver.quoteIdentifier(name)}"})
    }

    override def dropUserArtifacts(implicit session: profile.Backend#Session) = {
      blockingRunOnSession { implicit ec =>
        for {
          tables <- localTables
          _ <- DBIO.seq(tables.map(t => sqlu"""drop table #$t"""): _*)
          sequences <- localSequences
          _ <- DBIO.seq(sequences.map(s => sqlu"drop sequence #$s"): _*)
        } yield ()
      }
    }

    override lazy val capabilities = driver.capabilities + TestDB.capabilities.plainSql
  }
}
