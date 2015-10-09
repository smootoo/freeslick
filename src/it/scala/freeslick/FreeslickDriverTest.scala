package freeslick

import com.typesafe.slick.testkit.util._
import org.junit.runner.RunWith

class FreeslickDriverTest(driver: TestDB) extends DriverTest(driver) {
  override def tests: Seq[Class[_ <: GenericTest[_ >: Null <: TestDB]]] = {
    super.tests :+
      classOf[BooleanTest] :+
      classOf[FreeslickJoinTest] :+
      classOf[FreeslickSubqueryTest] :+
      classOf[UUIDTest] :+
      classOf[FetchOffsetTest] :+
      classOf[FreeslickGroupByTest]
    //TODO Sue timestamps, blobs, multiple autoinc column
  }
}

// These are here for testing any new driver tests
@RunWith(classOf[Testkit])
class H2MemTest extends FreeslickDriverTest(StandardTestDBs.H2Mem)

@RunWith(classOf[Testkit])
class H2DiskTest extends FreeslickDriverTest(StandardTestDBs.H2Disk)

@RunWith(classOf[Testkit])
class HsqldbMemTest extends FreeslickDriverTest(StandardTestDBs.HsqldbMem)

@RunWith(classOf[Testkit])
class HsqldbDiskTest extends FreeslickDriverTest(StandardTestDBs.HsqldbDisk)

@RunWith(classOf[Testkit])
class SQLiteMemTest extends FreeslickDriverTest(StandardTestDBs.SQLiteMem)

@RunWith(classOf[Testkit])
class SQLiteDiskTest extends FreeslickDriverTest(StandardTestDBs.SQLiteDisk)

@RunWith(classOf[Testkit])
class DerbyMemTest extends FreeslickDriverTest(StandardTestDBs.DerbyMem)

@RunWith(classOf[Testkit])
class DerbyDiskTest extends FreeslickDriverTest(StandardTestDBs.DerbyDisk)

@RunWith(classOf[Testkit])
class HeapTest extends FreeslickDriverTest(StandardTestDBs.Heap)

