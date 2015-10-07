package freeslick.profile.utils

import java.util.UUID

import org.scalatest.FunSuite
import slick.SlickException

class UUIDParserTest extends FunSuite {
  test("Parsing of uuid strings") {
    val randomUuid = UUID.randomUUID()
    val testString = randomUuid.toString.replaceAll("-", "")
    assert(randomUuid == UUIDParser.dashlessStringToUUID(testString), s"failed to match $randomUuid $testString")
    intercept[SlickException](UUIDParser.dashlessStringToUUID(""))
    intercept[SlickException](UUIDParser.dashlessStringToUUID("x"))
    intercept[SlickException](UUIDParser.dashlessStringToUUID(testString + "x"))
    intercept[SlickException](UUIDParser.dashlessStringToUUID(testString.substring(1)))
  }
}
