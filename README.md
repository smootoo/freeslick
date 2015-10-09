|Driver|Build status|
|------|-----------:|
|SQLServer 2008|[![Build status](https://ci.appveyor.com/api/projects/status/mdrfd7o7067c5vcm/branch/master?svg=true)](https://ci.appveyor.com/project/smootoo/freeslick/branch/master)|
|Oracle 11g|[![Build Status](https://travis-ci.org/smootoo/freeslick.svg?branch=master)](https://travis-ci.org/smootoo/freeslick/branches)|

[![Coverage Status](https://coveralls.io/repos/smootoo/freeslick/badge.svg?branch=master)](https://coveralls.io/r/smootoo/freeslick?branch=master)

# FreeSlick

Available on [maven central](http://search.maven.org/#artifactdetails|org.suecarter|freeslick_2.11|3.1.0|jar)

[Free software](https://www.gnu.org/philosophy/free-sw.html)
continuation of the [Slick](http://slick.typesafe.com/) MS-SQL driver,
with a full suite of integration tests against actual MS-SQL (2008) instances.
We did have tests running against 2000 and 2005, but it's hard to find CI
environments to test against and they are quite old. Let us know if you
need a driver maintained for those versions.

We have added an Oracle driver as well now. This has been written from scratch.

If there is some driver combination you are looking for, that isn't covered yet. Let us know.

# Usage

As is normal in Slick, import the "driver" (really these are profiles)
explicitly in order to get access to the `Database` instance:

```scala
import freeslick.MSSQLServerProfile.api._
```

The Oracle profile is at freeslick.OracleProfile

and don't forget to pass your actual JDBC driver when creating a connection, e.g.

```scala
val db = Database.forURL(url, driver = "net.sourceforge.jtds.jdbc.Driver")
```


The artefact is published as:

```scala
"org.suecarter" %% "freeslick" % "3.1.0"
```

# Integration Tests

We are using AppVeyor to automate MSSQLServer tests. (https://ci.appveyor.com/project/smootoo/freeslick)

To run the tests locally on a docker image, [follow the instructions on the Wiki](https://github.com/smootoo/freeslick/wiki/Locally-running-the-Integration-Tests).

Travis and docker drive the Oracle tests. You can fire up the Oracle 11g docker image to test locally. 
Check the .travis.yml file. 

We leverage the excellent Slick integration tests to validate our drivers and add some of our own
to test specific driver functionality.

# History

In version 2.0 of Slick,
[Typesafe removed support for their trivial MS-SQL driver](https://github.com/slick/slick/commit/e1f38fdcaa0e1105f9980c81a945e2ea27f4eb56#diff-50d3fdf1ae11ed9fd46016fbb8271858), [closed the source and started to charge for it](http://slick.typesafe.com/doc/2.0.0/extensions.html). BSD and Apache OSS licenses allow such moves: this incident serves as a good reason for you to prefer a [Copyleft](https://en.wikipedia.org/wiki/Copyleft) license (e.g. LGPL) that cannot be closed in your next [Free Software](http://www.gnu.org/philosophy/free-sw.en.html) project.

Anybody who works in a corporate environment knows the challenges involved in getting approval to buy any form of license - no matter the cost - so this strategic move from Typesafe is not only concerning, but frustrating for anybody using a Scala stack.

This project offers community-maintained support for MS-SQL and is willing to take contributions for other proprietary databases. Although, bizarrely, the [MS Access](https://github.com/slick/slick/blob/2.0.3/src/main/scala/scala/slick/driver/AccessDriver.scala) driver has remained part of Slick releases.

Our code starts with [the last-known release of the driver from Slick](https://github.com/slick/slick/blob/b70a2c7289e9aa4f6e12cf7426c5a91d47e1b4bf/src/main/scala/scala/slick/driver/SQLServerDriver.scala) and all community enhancements are made using the [LGPL](http://opensource.org/licenses/lgpl-3.0.html) so that they remain free (in both senses: user freedom and gratis).

# Contributing

You can contribute by clicking the star button and using this software! Tweet about it if you find it useful.

Contributors are encouraged to fork this repository and issue pull requests. It's such a simple project that if you have a bug, you'll probably be able to fix it yourself.
