package freeslick.testkit

import com.typesafe.slick.testkit.util.{AsyncTest, JdbcTestDB}

class FreeslickSubqueryTest extends AsyncTest[JdbcTestDB] {

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
      q3 = as.filter(_.id === as.sortBy(_.id).map(_.id).max)
      _ <- q3.result.named("q3").map(_ shouldBe Vector(42))
      q4 = as.filter(_.id in as.map(_.id)).sortBy(_.id)
      _ <- q4.result.named("q4").map(_ shouldBe Vector(42))
      q5 = as.filter(_.id in
        as.filter(_.id in as.sortBy(_.id).map(_.id)).sortBy(_.id).
          map(_.id)).sortBy(_.id)
      _ <- q5.result.named("q5").map(_ shouldBe Vector(42))
      q6 = as.filter(_.id in
        as.filter(_.id in as.sortBy(_.id).map(_.id)).
          sortBy(_.id).map(_.id)).sortBy(_.id).min
      _ <- q6.result.named("q6").map(_ shouldBe Some(42))
      _ <- as ++= Seq(43, 44, 45)
      // Correlated subquery
      qCorrelated1 = as.filter(x => x.id > as.filter(_.id === x.id).min).sortBy(_.id)
      _ <- qCorrelated1.result.map(_ shouldBe Seq())
      qCorrelated2 = as.filter(x => x.id >= as.filter(_.id === x.id).min).sortBy(_.id)
      _ <- qCorrelated2.result.map(_ shouldBe Seq(42, 43, 44, 45))
    } yield ()
  }
}
