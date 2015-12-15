package freeslick.testkit

import com.typesafe.slick.testkit.util.{AsyncTest, JdbcTestDB}

class FreeslickInsertTest extends AsyncTest[JdbcTestDB] {

  import tdb.profile.api._

  class T(tableName: String)(tag: Tag) extends Table[(Int, String)](tag, tableName) {
    def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    def name = column[String]("NAME")
    def * = (id, name)
    def ins = (id, name)
  }
  // Oracle 11g doesn't seem to return batch counts. Should be in 12c though
  lazy val batchReturnCounts = !tdb.jdbcDriver.contains("OracleDriver")

  def testUpsertAutoIncReturning = {
    val ts = TableQuery(new T("T_UPSERT")(_))
    (for {
      _ <- ts.schema.create
      q1 = ts returning ts.map(_.id)
      _ <- ifCap(jcap.returnInsertKey) {
        for {
          // Single insert returns single auto inc value
          _ <- (q1 +=(0, "e")).map(_ shouldBe 1)
          _ <- (q1 +=(0, "f")).map(_ shouldBe 2)
          _ <- ts.sortBy(_.id).result.map(_ shouldBe Seq((1, "e"), (2, "f")))
        } yield ()
      }
      // Inserts without "returning" are row counts of inserts
      _ <- (ts ++= Seq((1, "a"), (2, "b"))).map(_ shouldBe (if (batchReturnCounts) Some(2) else None))
      _ <- ts.insertOrUpdate((0, "c")).map(_ shouldBe 1)
      _ <- ts.insertOrUpdate((1, "d")).map(_ shouldBe 1)
      _ <- ifCap(jcap.returnInsertKey) {
        for {
          _ <- ts.sortBy(_.id).result.map(_ shouldBe Seq((1, "d"), (2, "f"), (3, "a"), (4, "b"), (5, "c")))
          // Upserts with returning
          _ <- q1.insertOrUpdate((0, "g")).map(_ shouldBe Some(6)) // insert returns key
          _ <- q1.insertOrUpdate((1, "f")).map(_ shouldBe None) // update returns none
          _ <- ts.sortBy(_.id).result.map(_ shouldBe Seq((1, "f"), (2, "f"), (3, "a"), (4, "b"), (5, "c"), (6, "g")))
          // batch inserts return sequences of inserted keys
          _ <- (q1 ++= Seq((1, "a"), (2, "b"))).map(_ shouldBe Seq(7,8))
          _ <- (q1 ++= (0 to 30).map(x => (0, "x"))).map(_ shouldBe (0 to 30).map(_+9))
        } yield ()
      }
    } yield ()).withPinnedSession
  }
  def testUpsertAutoIncNonReturning = {
    val ts = TableQuery(new T("T_UPSERTNORET")(_))
    (for {
      _ <- ts.schema.create
      _ <- ifCap(jcap.returnInsertKey) {
        for {
          _ <- (ts +=(0, "e")).map(_ shouldBe 1)
          _ <- (ts +=(0, "f")).map(_ shouldBe 1)
          _ <- ts.sortBy(_.id).result.map(_ shouldBe Seq((1, "e"), (2, "f")))
        } yield ()
      }
      _ <- ts ++= Seq((1, "a"), (2, "b"))
      _ <- ts.insertOrUpdate((0, "c")).map(_ shouldBe 1)
      _ <- ts.insertOrUpdate((1, "d")).map(_ shouldBe 1)
      _ <- ifCap(jcap.returnInsertKey) {
        for {
          _ <- ts.sortBy(_.id).result.map(_ shouldBe Seq((1, "d"), (2, "f"), (3, "a"), (4, "b"), (5, "c")))
          _ <- ts.insertOrUpdate((0, "g")).map(_ shouldBe 1) // insert returns key
          _ <- ts.insertOrUpdate((1, "f")).map(_ shouldBe 1) // update returns none
          _ <- ts.sortBy(_.id).result.map(_ shouldBe Seq((1, "f"), (2, "f"), (3, "a"), (4, "b"), (5, "c"), (6, "g")))
          _ <- (ts ++= (0 to 30).map(x => (0, "x"))).map(_ shouldBe (if (batchReturnCounts) Some(31) else None))
        } yield ()
      }
    } yield ()).withPinnedSession
  }
}
