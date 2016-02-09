package freeslick.profile

import com.typesafe.slick.testkit.tests._
import com.typesafe.slick.testkit.util._
import freeslick.testkit._
import org.junit.runner.RunWith

class FreeslickDriverTest(driver: TestDB) extends DriverTest(driver) {
  override def tests: Seq[Class[_ <: GenericTest[_ >: Null <: TestDB]]] = {
    super.tests :+
      classOf[BooleanTest] :+
      classOf[FetchOffsetTest] :+
      classOf[FreeslickGroupByTest] :+
      classOf[FreeslickInsertTest] :+
      classOf[FreeslickClobTest] :+
      classOf[FreeslickJoinTest] :+
      classOf[FreeslickMutateTest] :+
      classOf[FreeslickSubqueryTest] :+
      classOf[JDBCFunctionTest] :+
      classOf[UUIDTest]
    //TODO Sue timestamps, blobs, all the jdbcfunctions
    //Seq(classOf[FreeslickInsertTest])
  }
}

// These are here for testing any new driver tests
@RunWith(classOf[Testkit])
class H2MemTest extends FreeslickDriverTest(StandardTestDBs.H2Mem)

@RunWith(classOf[Testkit])
class HsqldbMemTest extends FreeslickDriverTest(StandardTestDBs.HsqldbMem)

@RunWith(classOf[Testkit])
class SQLiteMemTest extends FreeslickDriverTest(StandardTestDBs.SQLiteMem)

@RunWith(classOf[Testkit])
class DerbyMemTest extends FreeslickDriverTest(StandardTestDBs.DerbyMem)

@RunWith(classOf[Testkit])
class HeapTest extends FreeslickDriverTest(StandardTestDBs.Heap)

