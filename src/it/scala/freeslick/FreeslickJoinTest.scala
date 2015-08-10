package freeslick

import com.typesafe.slick.testkit.tests.JoinTest
import com.typesafe.slick.testkit.util.{AsyncTest, JdbcTestDB}

class FreeslickJoinTest extends JoinTest {

  import tdb.driver.api._
  override def testZip = ifCap(rcap.zip) {
    class Categories(tag: Tag) extends Table[(Int, String)](tag, "cat_z") {
      def id = column[Int]("id")
      def name = column[String]("name")
      def * = (id, name)
    }
    val categories = TableQuery[Categories]

    class Posts(tag: Tag) extends Table[(Int, String, Int)](tag, "posts_z") {
      def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
      def title = column[String]("title")
      def category = column[Int]("category")
      def * = (id, title, category)
    }
    val posts = TableQuery[Posts]

    for {
      _ <- (categories.schema ++ posts.schema).create
      _ <- categories ++= Seq(
        (1, "Scala"),
        (3, "Windows"),
        (2, "ScalaQuery"),
        (4, "Software")
      )
      _ <- posts.map(p => (p.title, p.category)) ++= Seq(
        ("Test Post", -1),
        ("Formal Language Processing in Scala, Part 5", 1),
        ("Efficient Parameterized Queries in ScalaQuery", 2),
        ("Removing Libraries and HomeGroup icons from the Windows 7 desktop", 3),
        ("A ScalaQuery Update", 2)
      )
      q1 = for {
        (c, i) <- categories.sortBy(_.id).zipWithIndex
      } yield (c.id, i)
      _ <- q1.result.map(_ shouldBe List((1,0), (2,1), (3,2), (4,3)))
      q2 = for {
        (c, p) <- categories.sortBy(_.id) zip posts.sortBy(_.category)
      } yield (c.id, p.category)
      _ <- q2.result.map(_ shouldBe List((1,-1), (2,1), (3,2), (4,2)))
    // The main slick zip testkit code assers on being able to zip tables together
    // in insertion order. That definitely doesn't work for Oracle and I think
    // it makes more sense in a relational world to sort first before zipping.
      q4 = for {
        res <- categories.sortBy(_.id).zipWith(posts.sortBy(_.category),
          (c: Categories, p: Posts) => (c.id, p.category))
      } yield res
      _ <- q4.result.map(_ shouldBe List((1,-1), (2,1), (3,2), (4,2)))
      q5 = for {
        (c, i) <- categories.sortBy(_.id).zipWithIndex
      } yield (c.id, i)
      _ <- q5.result.map(_ shouldBe List((1,0), (2,1), (3,2), (4,3)))
      q6 = for {
        ((c, p), i) <- (categories.sortBy(_.id) zip posts.sortBy(_.category)).sortBy{
          case (c, p) => (c.id, p.category)
        }.zipWithIndex
      } yield (c.id, p.category, i)
      _ <- q6.result.map(_ shouldBe List((1, -1, 0), (2, 1, 1), (3, 2, 2), (4, 2, 3)))
    } yield ()
  }

}
