|Database|JDBC Driver|Freeslick Profile|Build status|
|--------|-----------|-----------------|-----------:|
|SQLServer 2008, 2012, 2014|[jtds:1.2.8](http://sourceforge.net/projects/jtds/files/jtds/)|freeslick.MSSQLServerProfile|[![Build status](https://ci.appveyor.com/api/projects/status/mdrfd7o7067c5vcm?svg=true&branch=master)](https://ci.appveyor.com/project/smootoo/freeslick)|
|SQLServer 2008, 2012, 2014|[msjdbc:4.2](https://www.microsoft.com/en-gb/download/details.aspx?id=11774)|freeslick.MSJDBCSQLServerProfile|[![Build status](https://ci.appveyor.com/api/projects/status/mdrfd7o7067c5vcm?svg=true&branch=master)](https://ci.appveyor.com/project/smootoo/freeslick)|
|Oracle 11g|[ojdbc7:12.1.0.2](http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html)|freeslick.OracleProfile|[![Build Status](https://travis-ci.org/smootoo/freeslick.svg?branch=master)](https://travis-ci.org/smootoo/freeslick)|
|DB2 10.5|[db2jcc4:4.19.20](http://www-01.ibm.com/support/docview.wss?uid=swg21363866)|freeslick.DB2Profile|[![Build Status](https://travis-ci.org/smootoo/freeslick.svg?branch=master)](https://travis-ci.org/smootoo/freeslick)|

[![Coverage Status](https://coveralls.io/repos/smootoo/freeslick/badge.svg?branch=master)](https://coveralls.io/r/smootoo/freeslick?branch=master)

# FreeSlick

Available on [maven central](http://search.maven.org/#artifactdetails|org.suecarter|freeslick_2.11|3.1.1.1|jar)

[Free software](https://www.gnu.org/philosophy/free-sw.html)
continuation of the [Slick](http://slick.typesafe.com/) MS-SQL driver,
with a full suite of integration tests against actual MS-SQL (2008) instances.
We did have tests running against 2000 and 2005, but it's hard to find CI
environments to test against and they are quite old. Let us know if you
need a driver maintained for those versions.

We have now added Oracle and DB2 profiles as well. These have been written from scratch.

If there is some driver/database combination you are looking for, that isn't covered yet. Let us know.

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
"org.suecarter" %% "freeslick" % "3.1.1.1"
```

# Integration Tests

We are using AppVeyor to automate MSSQLServer tests. (https://ci.appveyor.com/project/smootoo/freeslick)

To run the tests locally on a docker image, [follow the instructions on the Wiki](https://github.com/smootoo/freeslick/wiki/Locally-running-the-Integration-Tests).

Travis and docker drive the Oracle and DB2 tests. You can fire up the Oracle or DB2 docker images to test locally. 
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

## Getting the dependencies right

If you want to contribute to the freeslick project, you first must install some dependencies before you can run the tests. This ascii cast shows how to do this.

Instructions:

1. First you must have docker running. For Windows and Mac, use [boot2docker](https://github.com/boot2docker/boot2docker) or docker-machine for [Mac](https://docs.docker.com/engine/installation/mac/) or [Windows](https://docs.docker.com/engine/installation/windows/). For Linux use the packet manager of your distribution
2. Install the official docker image for DB2 `docker run -d -p 50000:50000 --name db2freeslick -e DB2INST1_PASSWORD=db2inst1-pwd -e LICENSE=accept  ibmcom/db2express-c:latest "db2start"
`
3. Copy the needed driver file out of that image `docker cp db2freeslick:/home/db2inst1/sqllib/java/db2jcc4.jar .` ...
4. ... and install it in the Maven cache on your local machine `mvn install:install-file -DgroupId=com.ibm -DartifactId=db2jcc4 -Dversion=4.19.26 -Dpackaging=jar -Dfile=./db2jcc4.jar`
5. Please download the Oracle JDBC driver manually from [Oracle](http://www.oracle.com/technetwork/database/features/jdbc/jdbc-drivers-12c-download-1958347.html). Fetch the file called "ojdbc7.jar" (3,397,734 bytes, SHA1 Checksum: a2348e4944956fac05235f7cd5d30bf872afb157)`. You will need to create a free Oracle account for this.
6. Copy the file from your download directory `cp ~/Downloads/ojdbc7.jar ./ojdbc7-12.1.0.2.jar` and `install it in the Maven cache on your local machine
mvn install:install-file -DgroupId=com.oracle -DartifactId=ojdbc7 -Dversion=12.1.0.2 -Dpackaging=jar -Dfile=./ojdbc7-12.1.0.2.jar`

After that `sbt test` should run without errors.

Please note that `sbt test` will load the JDBC drivers, but not run the tests against the proprietary databases. It will use an in-memory H2 database. It will only test if you have all the dependencies right and are ready to run. To run the tests against the real databases, run `sbt it:test`.



For detailed instructions, please see the following [screen cast](https://asciinema.org/a/31670?speed=2)
[![asciicast](https://asciinema.org/a/31670.png)](https://asciinema.org/a/31670?speed=2)
