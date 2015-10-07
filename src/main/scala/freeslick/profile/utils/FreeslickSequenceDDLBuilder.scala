package freeslick.profile.utils

import slick.driver.JdbcDriver

trait FreeslickSequenceDDLBuilder {
  jdbcDriver: JdbcDriver =>

  def buildSeqDDL(seq: Sequence[_]): DDL = {
    val b = new StringBuilder append "create sequence " append quoteIdentifier(seq.name)
    seq._increment.foreach { i =>
      b append " increment by " append i
    }
    seq._minValue.foreach { m =>
      b append " minvalue " append m
    }
    seq._maxValue.foreach { m =>
      b append " maxvalue " append m
    }
    seq._start.foreach { s =>
      b append " start with " append s
    }
    if (seq._cycle) {
      b append " cycle"
      //TODO Sue add nocache/cache size option
      val cacheSize = 20 // Oracle default http://www.dba-oracle.com/t_sequence_caching.htm
      for {
        maxValue <- seq._maxValue
        minValue <- seq._minValue
      } yield {
        try {
          val cycleSize = math.abs(maxValue.toString.toInt - minValue.toString.toInt)
          if (cacheSize > cycleSize) b append " cache " append cycleSize
        } catch {
          case _: Exception => //if max and min aren't convertible to ints, nothing to put here
        }
      }
    }
    DDL(b.toString, "drop sequence " + quoteIdentifier(seq.name))
  }
}
