package freeslick.testkit

import com.typesafe.slick.testkit.util.{AsyncTest, JdbcTestDB}

class BooleanTest extends AsyncTest[JdbcTestDB] {

  import tdb.driver.api._

  def testBoolean = {
    class BooleanData(name: String)(tag: Tag) extends Table[(Int, Boolean, Option[Boolean])](tag, name) {
      def id = column[Int]("id")
      def data = column[Boolean]("data", O Default true)
      def optData = column[Option[Boolean]]("optData", O Default None)
      def * = (id, data, optData)
    }
    val xs = TableQuery(new BooleanData("booleanx")(_))
    val ys = TableQuery(new BooleanData("booleany")(_))
    val sqlTrue = slickDriver.columnTypes.booleanJdbcType.valueToSQLLiteral(true)
    val sqlFalse = slickDriver.columnTypes.booleanJdbcType.valueToSQLLiteral(false)
    for {
      // Casting to and from booleans
      _ <- LiteralColumn(true).asColumnOf[Int].result.
        map(_ shouldBe sqlTrue.toInt)
      _ <- LiteralColumn(false).asColumnOf[String].result.
        map(_ shouldBe sqlFalse.toString)
      _ <- LiteralColumn(sqlFalse).asColumnOf[Boolean].result.
        map(_ shouldBe false)

      _ <- (xs.schema ++ ys.schema).create
      _ <- xs ++= Seq(true, true, false, true).zipWithIndex.map { case (data, id) => (id + 10, data, if (data) Some(data) else None) }
      _ <- ys ++= Seq(true, false, false, true).zipWithIndex.map { case (data, id) => (id + 11, data, if (!data) Some(data) else None) }

      _ <- xs.map(_.data).result.map(_ shouldBe Seq(true, true, false, true))
      // Boolean filter
      q1 = xs.filter(_.data === true).to[Set]
      r1 <- q1.result
      _ = r1 shouldBe Set((10, true, Some(true)), (11, true, Some(true)), (13, true, Some(true)))
      _ <- ys.filter(_.data === true).map(_.id).result.map(_ shouldBe Seq(11, 14))
      // optional boolean filtering
      _ <- ys.filter(_.optData === true).map(_.id).result.map(_ shouldBe Seq())
      _ <- ys.filter(_.optData === false).map(_.id).result.map(_ shouldBe Seq(12, 13))
      _ <- ys.filter(_.optData.isEmpty).map(_.id).result.map(_ shouldBe Seq(11, 14))

      //bind variables
      _ <- ys.filter(_.data === true.bind).map(_.id).result.map(_ shouldBe Seq(11, 14))
      _ <- ys.filter(_.data === false.bind).map(_.id).result.map(_ shouldBe Seq(12, 13))
      // note - use isEmpty to filter where = null
      _ <- ys.filter(_.optData === Seq[Boolean]().headOption.bind).map(_.id).result.map(_ shouldBe Seq())

      // inner,left,right,outer joins with boolean
      r2 <- (xs.filter(_.data === true) joinLeft ys on (_.id === _.id)).to[Set].result
      _ = r2 shouldBe Set(
        ((10, true, Some(true)), None),
        ((11, true, Some(true)), Some((11, true, None))),
        ((13, true, Some(true)), Some((13, false, Some(false)))))
      r3 <- (xs.filter(_.data === true) joinRight ys on (_.id === _.id)).to[Set].result
      _ = r3 shouldBe Set(
        (Some((11, true, Some(true))), (11, true, None)),
        (None, (12, false, Some(false))),
        (Some((13, true, Some(true))), (13, false, Some(false))),
        (None, (14, true, None)))
      r4 <- (xs.filter(_.data === true) join ys on (_.id === _.id)).to[Set].result
      _ = r4 shouldBe Set(
        ((11, true, Some(true)), (11, true, None)),
        ((13, true, Some(true)), (13, false, Some(false))))
      r5 <- (xs.filter(_.data === true) joinFull ys on (_.id === _.id)).to[Set].result
      _ = r5 shouldBe Set(
        (None, Some((14, true, None))),
        (Some((13, true, Some(true))), Some((13, false, Some(false)))),
        (Some((10, true, Some(true))), None),
        (None, Some((12, false, Some(false)))),
        (Some((11, true, Some(true))), Some((11, true, None))))

      // in clauses
      _ <- xs.filter(_.data inSetBind Seq()).map(_.id).result.map(_ shouldBe Nil)
      _ <- xs.filter(_.data inSetBind Seq(true)).map(_.id).result.map(_ shouldBe Seq(10, 11, 13))
      _ <- xs.filter(_.data inSetBind Seq(true, false)).map(_.id).result.map(_ shouldBe Seq(10, 11, 12, 13))
      _ <- xs.filter(_.optData inSetBind Seq(true, false)).map(_.id).result.map(_ shouldBe Seq(10, 11, 13))
      _ <- xs.filter(_.data inSet Seq()).map(_.id).result.map(_ shouldBe Nil)
      _ <- xs.filter(_.data inSet Seq(true)).map(_.id).result.map(_ shouldBe Seq(10, 11, 13))
      _ <- xs.filter(_.data inSet Seq(true, false)).map(_.id).result.map(_ shouldBe Seq(10, 11, 12, 13))
      _ <- xs.filter(_.optData inSet Seq(true, false)).map(_.id).result.map(_ shouldBe Seq(10, 11, 13))

      // boolean library operations
      // LiteralColumns
      _ <- LiteralColumn(true).result.map(_ shouldBe true)
      _ <- (LiteralColumn(true) && LiteralColumn(false)).result.map(_ shouldBe false)
      _ <- (LiteralColumn(true) && LiteralColumn[Option[Boolean]](Some(true))).result.map(_ shouldBe Some(true))
      // Some boolean operations on a nullable column are not right yet need to get 3 value logic right
//      _ <- (LiteralColumn(true) && LiteralColumn[Option[Boolean]](None)).result.map(_ shouldBe None)
//      _ <- (!LiteralColumn[Option[Boolean]](None)).result.map(_ shouldBe None)
//      _ <- (LiteralColumn(true) || LiteralColumn[Option[Boolean]](None)).result.map(_ shouldBe Some(true))
        _ <- (!LiteralColumn[Option[Boolean]](Some(false))).result.map(_ shouldBe Some(true))
//      r6 <- (xs.filter(_.data === true) joinLeft ys on (_.id === _.id)).map { case (l, optR) => {
//        (l, optR,
//          l.data && l.optData,
//          optR.map(r => (
//            r.data && r.optData,
//            l.data && r.data,
//            l.optData || r.optData,
//            !r.data,
//            !r.optData)))
//      }
//      }.result
//      _ = r6 shouldBe Seq(
//        ((10, true, Some(true)), None, Some(true), None),
//        ((11, true, Some(true)), Some((11, true, None)), Some(true), Some((Some(false), true, Some(true), false, false))),
//        ((13, true, Some(true)), Some((13, false, Some(false))), Some(true), Some((Some(false), false, Some(true), true, true))))
      // simple boolean filter
      _ <- xs.filter(_.data).map(_.id).result.map(_ shouldBe Seq(10, 11, 13))
      _ <- xs.filter(!_.data).map(_.id).result.map(_ shouldBe Seq(12))
      // exists type boolean operations
      _ <- xs.exists.result.map(_ shouldBe true)
      _ <- xs.filter(_.id === 23).exists.result.map(_ shouldBe false)
    } yield {}
  }
}
