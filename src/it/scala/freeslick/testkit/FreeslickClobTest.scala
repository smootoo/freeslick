package freeslick.testkit

import java.io.BufferedReader
import java.sql.Clob
import javax.sql.rowset.serial.SerialClob

import com.typesafe.slick.testkit.util.{AsyncTest, JdbcTestDB}

import scala.util.Random

case class LongString(value: String) extends AnyVal

class FreeslickClobTest extends AsyncTest[JdbcTestDB] {

  import tdb.profile.api._

  implicit def jsValueTypedType = MappedColumnType.base[LongString, Clob](
    { jsv => new SerialClob(jsv.value.toCharArray) },
    { clob =>
      val reader = new BufferedReader(clob.getCharacterStream)
      LongString(Stream.continually(reader.readLine()).takeWhile(_ != null).mkString)
    }
  )

  class T(tableName: String)(tag: Tag) extends Table[(Int, LongString)](tag, tableName) {
    def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    def name = column[LongString]("NAME", O.SqlType("CLOB"))
    def * = (id, name)
  }

  def longCharSeq = Random.alphanumeric.take(10000).toString

  def testClobMapping = {
    val ts = TableQuery(new T("T_CLOB")(_))
    (for {
      _ <- ts.schema.create
      _ <- ifCap(jcap.returnInsertKey) {
        val data = longCharSeq
        for {
          // Single insert returns single auto inc value
          _ <- ts +=(0, LongString(data))
          _ <- ts.sortBy(_.id).result.map{ s =>
            s.length shouldBe 1
            s.head._1 shouldBe 1
            s.head._2.value shouldBe data
          }
        } yield ()
      }
    } yield ()).withPinnedSession
  }
}
