package freeslick

import java.util.UUID
import org.scalatest._
import org.slf4j.LoggerFactory
import slick.driver.JdbcProfile
import slick.jdbc.{ StaticQuery => Q }
import slick.jdbc.StaticQuery.interpolation

object MSSQL extends Tag("MSSQL")

/**
 * Assumes that an MS-SQL instance is available at
 *
 *    jdbc:jtds:sqlserver://localhost:<PORT>
 *
 * with an admin account having username "sa" and empty password.
 *
 * To provision an MS-SQL on Linux / OS X, run the bundled `.mssql.sh`
 * script. On Windows, click some things.
 *
 * A randomly-named database will be created for use in the tests, and
 * will be optional dropped after all the tests have completed.
 */
abstract class MSSQLFixtureSpec extends fixture.WordSpecLike with BeforeAndAfterAll {
  private lazy val log = LoggerFactory.getLogger(getClass)

  private val base = "jdbc:jtds:sqlserver://localhost:" + port
  def port = 2000
  private val dbname = s"FreeSlick_${UUID.randomUUID()}".replace("-", "")
  private val url = s"$base/$dbname"
  private val user = "sa"
  private val password = "FreeSlick"

  val driver = "net.sourceforge.jtds.jdbc.Driver"
  val profile: JdbcProfile = MSSQLProfile
  import profile.api._

  // bill made me do it! https://github.com/scalatest/scalatest/issues/504
  override def tags = (testNames.map(_ -> Set.empty[String]).toMap ++ super.tags).mapValues(_ + MSSQL.name)

  override def beforeAll(): Unit = {
    log.info(s"creating database $dbname on $base")
    val db = Database.forURL(base, user = user, password = password, driver = driver)
    db.run(SimpleDBIO{ implicit s => s"CREATE DATABASE $dbname"})
    log.info(s"created database $dbname")
  }

  override def afterAll(): Unit = {
    log.info(s"dropping database $dbname on $base")
    val db = Database.forURL(base, user = user, password = password, driver = driver)
    db.run(SimpleDBIO{ implicit s =>
      // NOTE: string interpolation does not work in DROP statements
      Q.updateNA(s"DROP DATABASE $dbname")
    })
    log.info(s"dropped database $dbname")
  }

  type FixtureParam = Database
  def withFixture(test: OneArgTest) = {
    log.info(s"starting test against $url")
    val db = Database.forURL(url, user = user, password = password, driver = driver)
    withFixture(test.toNoArgTest(db))
  }
}
