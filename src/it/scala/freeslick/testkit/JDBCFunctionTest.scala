package freeslick.testkit

import com.typesafe.slick.testkit.util.{JdbcTestDB, AsyncTest}

class JDBCFunctionTest extends AsyncTest[JdbcTestDB] {

  import tdb.driver.api._

  def testLibraryFunctions = {
    for {
      _ <- Functions.user.result
      _ <- Functions.database.result
      _ <- Functions.currentDate.result
      _ <- Functions.currentTime.result
      _ <- Functions.pi.result
    } yield {}
  }
}
