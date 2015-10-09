package freeslick

import com.typesafe.slick.testkit.util.{AsyncTest, RelationalTestDB}
import slick.driver.{H2Driver, PostgresDriver}

class FreeslickGroupByTest extends AsyncTest[RelationalTestDB] {

  import tdb.profile.api._

  def testGroupBy = {
    class T(tag: Tag) extends Table[(Int, Option[Int], Int)](tag, "t3") {
      def a = column[Int]("a")

      def b = column[Option[Int]]("b")

      def c = column[Int]("c")

      def * = (a, b, c)
    }
    val ts = TableQuery[T]

    db.run {
      ts.schema.create >>
        (ts ++= Seq((1, Some(1), 3), (1, Some(2), 3), (1, Some(3), 3))) >>
        (ts ++= Seq((2, Some(1), 3), (2, Some(2), 3), (2, Some(5), 8))) >>
        (ts ++= Seq((3, Some(1), 3), (3, Some(9), 8)))
    }.flatMap { _ =>
      val qfs1 = ts.groupBy(x => x.a).map {
        case (a, b) => (a, b.map(_.b.getOrElse(0)).sum)
      }
      val qfs2 = ts.map(x => (23, x.b)).groupBy(_._1).map {
        case (divA, b) => (divA, b.map(_._2.getOrElse(0)).sum)
      }
      val qfs3 = ts.map(x => (23, x.b)).subquery.groupBy(_._1).map {
        case (divA, b) => (divA, b.map(_._2.getOrElse(0)).sum)
      }
      //TODO Sue does need of subquery here mean a bug in slick?
      val qfs4 = ts.map(x => (x.a % 2, x.b)).subquery.groupBy(_._1).map {
        case (divA, b) => (divA, b.map(_._2.getOrElse(0)).sum)
      }
      val qfs5 = ts.map(x => (23, x.b, x.c)).groupBy(x => (x._1, x._3)).map {
              case (divA, b) => (divA, b.map(_._2.getOrElse(0)).sum)
            }
      val qfs6 = ts.map(x => (23, x.b, x.c+2)).groupBy(x => (x._1, x._3)).map {
              case (divA, b) => (divA, b.map(_._2.getOrElse(0)).sum)
            }
      val qfs7 = ts.map(x => (23, x.b, x.c*0)).groupBy(x => (x._1, x._3)).map {
              case (divA, b) => (divA, b.map(_._2.getOrElse(0)).sum)
            }
      val qfs8 = ts.map(x => (x.b, x.c*0)).groupBy(x => x._2).map {
              case (divA, b) => (divA, b.map(_._1.getOrElse(0)).sum)
            }
      db.run(for {
        _ <- mark("qfs1", qfs1.result).map(_ shouldBe Seq((1, Some(6)), (2, Some(8)), (3, Some(10))))
        _ <- mark("qfs2", qfs2.result).map(_ shouldBe Seq((23, Some(24))))
        _ <- mark("qfs3", qfs3.result).map(_ shouldBe Seq((23, Some(24))))
        _ <- mark("qfs4", qfs4.result).map(_.toSet shouldBe Set((0, Some(8)), (1, Some(16))))
        _ <- mark("qfs5", qfs5.result).map(_.toSet shouldBe Set(((23,3), Some(10)), ((23,8), Some(14))))
        _ <- mark("qfs6", qfs6.result).map(_.toSet shouldBe Set(((23,5), Some(10)), ((23,10), Some(14))))
        _ <- mark("qfs7", qfs7.result).map(_.toSet shouldBe Set(((23,0), Some(24))))
        _ <- mark("qfs8", qfs8.result).map(_.toSet shouldBe Set((0, Some(24))))
      } yield ())
    }
  }
}
