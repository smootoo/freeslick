package freeslick.testkit

import com.typesafe.slick.testkit.util.{AsyncTest, JdbcTestDB}

class FreeslickInsertTest extends AsyncTest[JdbcTestDB] {

  import tdb.profile.api._

  def testInsertOrUpdateAutoInc = {
    class T(tag: Tag) extends Table[(Int, String)](tag, "T_MERGE2") {
      def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)

      def name = column[String]("NAME")

      def * = (id, name)

      def ins = (id, name)
    }
    val ts = TableQuery[T]

    (for {
      _ <- ts.schema.create
      q1 = ts returning ts.map(_.id)
      _ <- ifCap(jcap.returnInsertKey) {
        for {
          _ <- (q1 +=(0, "e")).map(_ shouldBe 1)
          _ <- (q1 +=(0, "f")).map(_ shouldBe 2)
          _ <- ts.sortBy(_.id).result.map(_ shouldBe Seq((1, "e"), (2, "f")))
        } yield ()
      }
      _ <- ts ++= Seq((1, "a"), (2, "b"))
      _ <- ts.insertOrUpdate((0, "c")).map(_ shouldBe 1)
      _ <- ts.insertOrUpdate((1, "d")).map(_ shouldBe 1)
      _ <- ifCap(jcap.returnInsertKey) {
        for {
          _ <- ts.sortBy(_.id).result.map(_ shouldBe Seq((1, "d"), (2, "f"), (3, "a"), (4, "b"), (5, "c")))
          _ <- q1.insertOrUpdate((0, "g")).map(_ shouldBe Some(6)) // insert returns key
          _ <- q1.insertOrUpdate((1, "f")).map(_ shouldBe None) // update returns none
          _ <- ts.sortBy(_.id).result.map(_ shouldBe Seq((1, "f"), (2, "f"), (3, "a"), (4, "b"), (5, "c"), (6, "g")))
        } yield ()
      }
    } yield ()).withPinnedSession
  }
}
