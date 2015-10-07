package freeslick.profile.utils

import java.util.UUID

import slick.SlickException

object UUIDParser {
  def dashlessStringToUUID(uuidString: String): UUID = {
    def lsplit(pos: List[Int], s: String): String = {
      val sb = new StringBuilder
      var from = 0
      pos.foreach(i => {
        sb append s.substring(from, i) append "-"
        from = i
      })
      sb append s.substring(from)
      sb.toString()
    }
    if (uuidString.length != 32)
      throw new SlickException(s"Unable to turn $uuidString into a uuid, it should be 32 characters, but is ${uuidString.length}")
    try {
      val split = lsplit(List(8, 12, 16, 20), uuidString)
      UUID.fromString(split)
    } catch {
      case e: Exception => throw new SlickException(s"Unable to turn $uuidString into a uuid", e)
    }
  }
}
