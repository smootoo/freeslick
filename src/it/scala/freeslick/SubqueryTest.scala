package freeslick

import com.typesafe.slick.testkit.util.{AsyncTest, JdbcTestDB}

class SubqueryTest extends AsyncTest[JdbcTestDB] {

  import tdb.driver.api._

  def testSubquery = {
    class A(tag: Tag) extends Table[Int](tag, "A_subquery") {
      def id = column[Int]("id")

      def * = id
    }
    val as = TableQuery[A]

    for {
      _ <- as.schema.create
      _ <- as += 42

      q0 = as.filter(_.id === 42.bind).length
      _ <- q0.result.named("q0").map(_ shouldBe 1)

      q1 = Compiled { (n: Rep[Int]) =>
        as.filter(_.id === n).map(a => as.length)
      }
      // Lots of nested subqueries, with group operations
      _ <- q1(42).result.named("q1(42)").map(_ shouldBe List(1))
      q2 = as.filter(_.id in as.sortBy(_.id).map(_.id))
      _ <- q2.result.named("q2").map(_ shouldBe Vector(42))
      q2 = as.filter(_.id === as.sortBy(_.id).map(_.id).max)
      _ <- q2.result.named("q2").map(_ shouldBe Vector(42))
      q2 = as.filter(_.id in as.map(_.id)).sortBy(_.id)
      _ <- q2.result.named("q2").map(_ shouldBe Vector(42))
      q2 = as.filter(_.id in
        as.filter(_.id in as.sortBy(_.id).map(_.id)).sortBy(_.id).
          map(_.id)).sortBy(_.id)
      _ <- q2.result.named("q2").map(_ shouldBe Vector(42))
      q2 = as.filter(_.id in
        as.filter(_.id in as.sortBy(_.id).map(_.id)).
          sortBy(_.id).map(_.id)).sortBy(_.id).min
      _ <- q2.result.named("q2").map(_ shouldBe Some(42))
      _ <- as ++= Seq(43, 44, 45)
      // Correlated subquery
      qCorrelated1 = as.filter(x => x.id > as.filter(_.id === x.id).min).sortBy(_.id)
      _ <- qCorrelated1.result.map(_ shouldBe Seq())
      qCorrelated2 = as.filter(x => x.id >= as.filter(_.id === x.id).min).sortBy(_.id)
      _ <- qCorrelated2.result.map(_ shouldBe Seq(42, 43, 44, 45))
    } yield ()
  }
}
