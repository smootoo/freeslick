package freeslick

class MSSQLProfileSpec extends MSSQLFixtureSpec {

  "CREATE TABLE" should {
    "handle int" in { db =>
      fail("not written")
    }

    "handle String" in { db =>
      fail("not written")
    }

    "handle etc" in { db =>
      fail("not written")
    }
  }

  "INSERT" should {
    """Handle "Robert'); DROP TABLE Students;--" """ in { db =>
      fail("not written")
    }
  }

}
