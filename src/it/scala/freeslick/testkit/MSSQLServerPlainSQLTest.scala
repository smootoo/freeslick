// Copied from https://github.com/slick/slick/blob/3.0/slick-testkit/src/main/scala/com/typesafe/slick/testkit/tests/PlainSQLTest.scala
// and then tweaked for SQLServer
package freeslick.testkit

import com.typesafe.slick.testkit.util.{AsyncTest, JdbcTestDB}
import slick.jdbc.GetResult

class MSSQLServerPlainSQLTest extends AsyncTest[JdbcTestDB] {

  import tdb.driver.api._

  implicit val getUserResult = GetResult(r => new User(r.<<, r.<<))

  case class User(id: Int, name: String)

  def testInterpolation = ifCap(tcap.plainSql) {
    def userForID(id: Int) = sql"select id, name from USERS where id = $id".as[User]
    def userForIdAndName(id: Int, name: String) = sql"select id, name from USERS where id = $id and name = $name".as[User]

    val foo = "foo"
    val s1 = sql"select id from USERS where name = ${"szeiger"}".as[Int]
    val s2 = sql"select id from USERS where name = '#${"guest"}'".as[Int]
    val s3 = sql"select id from USERS where name = $foo".as[Int]
    val s4 = sql"select id from USERS where name = '#$foo'".as[Int]
    s1.statements.head shouldBe "select id from USERS where name = ?"
    s2.statements.head shouldBe "select id from USERS where name = 'guest'"
    s3.statements.head shouldBe "select id from USERS where name = ?"
    s4.statements.head shouldBe "select id from USERS where name = 'foo'"

    val create: DBIO[Int] = sqlu"create table USERS(ID int not null primary key, NAME varchar(255))"

    seq(
      //SQLServer returns -1 for updateCount on schema changing operations (like create table)
      create.map(x => x shouldBe -1),
      DBIO.fold((for {
        (id, name) <- List((1, "szeiger"), (0, "admin"), (2, "guest"), (3, "foo"))
      } yield sqlu"insert into USERS values ($id, $name)"), 0)(_ + _).map(_ shouldBe 4),
      sql"select id from USERS".as[Int].map(_.toSet shouldBe Set(0, 1, 2, 3)), //TODO Support `to` in Plain SQL Actions
      userForID(2).map(_.head shouldBe User(2, "guest")), //TODO Support `head` and `headOption` in Plain SQL Actions
      s1.map(_ shouldBe List(1)),
      s2.map(_ shouldBe List(2)),
      userForIdAndName(2, "guest").map(_.head shouldBe User(2, "guest")), //TODO Support `head` and `headOption` in Plain SQL Actions
      userForIdAndName(2, "foo").map(_.headOption shouldBe None) //TODO Support `head` and `headOption` in Plain SQL Actions
    )
  }
}
