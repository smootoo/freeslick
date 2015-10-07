package freeslick.testkit

import com.typesafe.slick.testkit.util.{AsyncTest, JdbcTestDB}

class FetchOffsetTest extends AsyncTest[JdbcTestDB] {

  import tdb.driver.api._

  def testFetchLimits = {
    class People(tag: Tag) extends Table[(String, Int)](tag, "people") {
      def name = column[String]("name")
      def age = column[Int]("age")
      def * = (name, age)
    }
    val people = TableQuery[People]

    for {
      _ <- people.schema.create
      data = Seq(
              ("A", 30),
              ("B", 25),
              ("C", 35),
              ("D", 15),
              ("E", 65),
              ("F", 75),
              ("G", 60),
              ("H", 61),
              ("I", 61)
            )
      _ <- people ++= data
      q0 = people.length
      _ <- q0.result.map(_ shouldBe data.size)
      q1 = people.filter(_.name <= "C").length
      _ <- q1.result.map(_ shouldBe 3)
      q2 = people.take(3).map(_.name)
    // take 3, could be any 3 though
      _ <- q2.result.map(_.size shouldBe 3)
      q2 = people.sortBy(_.age)
    // First 3 (make sure we take first 3 after sorting, not before)
      _ <- q2.take(3).map(_.age).result.map(_ shouldBe data.map(_._2).sorted.take(3))
    // 4 upto 7
      _ <- q2.drop(3).take(4).map(_.age).result.map(_ shouldBe data.map(_._2).sorted.slice(3,7))
    // 8 until a number far off the end
      _ <- q2.drop(7).take(50).map(_.age).result.map(_ shouldBe data.map(_._2).sorted.slice(7,57))
    // taking passed the end should be empty
      _ <- q2.drop(data.size).take(50).map(_.age).result.map(_ shouldBe Seq())
    } yield ()
  }
}
