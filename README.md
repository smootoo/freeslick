<!--
[![Build Status](https://api.shippable.com/projects/5504572d5ab6cc13529ad13e/badge?branchName=master)](https://app.shippable.com/projects/5504572d5ab6cc13529ad13e/builds/latest)
[![Coverage Status](https://coveralls.io/repos/fommil/freeslick/badge.svg?branch=master)](https://coveralls.io/r/fommil/freeslick?branch=master)
-->

# FreeSlick

[Free software](https://www.gnu.org/philosophy/free-sw.html)
continuation of the [Slick](http://slick.typesafe.com/) MS-SQL driver,
with a full suite of integration tests against actual MS-SQL (2000,
2005, 2008) instances.

# Usage

As is normal in Slick, import the "driver" (really these are profiles)
explicitly in order to get access to the `Database` instance:

```scala
import freeslick.MSSQLProfile.simple._
```

and don't forget to pass your actual JDBC driver when creating a connection, e.g.

```scala
val db = Database.forURL(url, driver = "net.sourceforge.jtds.jdbc.Driver")
```


The artefact (currently only snapshot) is published as:

```scala
resolvers += Resolver.sonatypeRepo("snapshots")

"com.github.fommil" %% "freeslick" % "2.0.3-SNAPSHOT"
```

# Integration Tests

We have a docker image that will start up MSSQL 2000, 2005 and 2008
and run the integration tests for your branch. We'd very much like to
automate this as part of the Pull Request review process, but
[we need a hardware donation](https://github.com/smootoo/freeslick/issues/11).

To run the tests locally, [follow the instructions on the Wiki](https://github.com/smootoo/freeslick/wiki/Locally-running-the-Integration-Tests).


# History

In version 2.0 of Slick,
[Typesafe removed support for their trivial MS-SQL driver](https://github.com/slick/slick/commit/e1f38fdcaa0e1105f9980c81a945e2ea27f4eb56#diff-50d3fdf1ae11ed9fd46016fbb8271858), [closed the source and started to charge for it](http://slick.typesafe.com/doc/2.0.0/extensions.html). BSD and Apache OSS licenses allow such moves: this incident serves as a good reason for you to prefer a [Copyleft](https://en.wikipedia.org/wiki/Copyleft) license (e.g. LGPL) that cannot be closed in your next [Free Software](http://www.gnu.org/philosophy/free-sw.en.html) project.

Anybody who works in a corporate environment knows the challenges involved in getting approval to buy any form of license - no matter the cost - so this strategic move from Typesafe is not only concerning, but frustrating for anybody using a Scala stack.

This project offers community-maintained support for MS-SQL and is willing to take contributions for other proprietary databases. Although, bizarrely, the [MS Access](https://github.com/slick/slick/blob/2.0.3/src/main/scala/scala/slick/driver/AccessDriver.scala) driver has remained part of Slick releases.

Our code starts with [the last-known release of the driver from Slick](https://github.com/slick/slick/blob/b70a2c7289e9aa4f6e12cf7426c5a91d47e1b4bf/src/main/scala/scala/slick/driver/SQLServerDriver.scala) and all community enhancements are made using the [LGPL](http://opensource.org/licenses/lgpl-3.0.html) so that they remain free (in both senses: user freedom and gratis).

# Contributing

You can contribute by clicking the star button and using this software! Tweet about it if you find it useful.

Contributors are encouraged to fork this repository and issue pull requests. It's such a simple project that if you have a bug, you'll probably be able to fix it yourself.
