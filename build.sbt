import sbt._
import ScoverageSbtPlugin.ScoverageKeys._

// NOTE: the following skips the MS-SQL tests
// it:test-only * -- -l MSSQL

organization := "org.suecarter"

name := "freeslick"

crossScalaVersions := Seq("2.11.5", "2.10.5")

version := "3.1.0.1-SNAPSHOT"

//resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.mavenLocal

configs(IntegrationTest)
inConfig(IntegrationTest)(Defaults.testSettings)
parallelExecution in IntegrationTest := false
testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-s", "-a", "-Djava.awt.headless=true")

libraryDependencies ++= Seq(
  "com.typesafe.slick"  %% "slick"                       % "3.1.0",
  "com.typesafe.slick"  %% "slick-testkit"               % "3.1.0"    % "test;it",
  "com.typesafe.slick"  %% "slick-hikaricp"              % "3.1.0"    % "test;it",
  "com.novocode"         % "junit-interface"             % "0.11"     % "test;it",
  "org.scalatest"       %% "scalatest"                   % "2.2.4"    % "test;it",
  "org.scalamock"       %% "scalamock-scalatest-support" % "3.2.1"    % "test;it",
  "org.scalacheck"      %% "scalacheck"                  % "1.12.2"   % "test;it",
  "ch.qos.logback"       % "logback-classic"             % "1.1.2"    % "test;it",
  "com.zaxxer"           % "HikariCP-java6"              % "2.3.7"    % "test;it",
  // jTDS 2.3.x is JDK 1.7+ so stick with 1.2.x
  "net.sourceforge.jtds" % "jtds"                        % "1.2.8"    % "optional;test;it",
  // add the below dependencies to test any new driver tests against other db drivers
  "com.h2database"       % "h2"                          % "1.3.170"  % "test;it",
  "org.apache.derby"     % "derby"                       % "10.9.1.0" % "test;it",
  "org.hsqldb"           % "hsqldb"                      % "2.2.8"    % "test;it"
) ++ sys.env.get("APPVEYOR").map(_ => Seq()).getOrElse(Seq( //Don't depend on non-public jars in APPVeyor environment
  "com.oracle"           % "ojdbc7"                      % "12.1.0.2" % "optional;test;it",
  "com.ibm"              % "db2jcc4"                     % "4.19.26"  % "optional;test;it"
))

scalacOptions in Compile ++= Seq(
  "-encoding", "UTF-8", "-target:jvm-1.6", "-feature", "-deprecation",
  "-Xfatal-warnings",
  "-language:postfixOps", "-language:implicitConversions"
)

javacOptions in (Compile, compile) ++= Seq (
  "-source", "1.6", "-target", "1.6", "-Xlint:all", "-Werror"
)

javacOptions in doc ++= Seq("-source", "1.6")

maxErrors := 1

fork := true

javaOptions ++= {
  Seq(
    "-XX:MaxPermSize=256m",
    "-Xmx2g",
    "-XX:+UseConcMarkSweepGC"
  )
}

// sbt 0.13.7 introduced awesomely fast resolution caching
updateOptions := updateOptions.value.withCachedResolution(true)

scalariformSettings

// let's bump this every time we get more tests
//coverageMinimum := 75
//coverageFailOnMinimum := true
coverageHighlighting := false

licenses := Seq(
  "LGPL 3.0" -> url("http://www.gnu.org/licenses/lgpl-3.0.txt"),
  "BSD 2 Clause" -> url("https://github.com/slick/slick/blob/b70a2c7289e9aa4f6e12cf7426c5a91d47e1b4bf/LICENSE.txt")
)

homepage := Some(url("http://github.com/smootoo/freeslick"))

val publishMasterOnTravis = taskKey[Unit]("publish master on travis")

def publishMasterOnTravisImpl = Def.taskDyn {
  import scala.util.Try
  val travis   = Try(sys.env("TRAVIS")).getOrElse("false") == "true"
  val pr       = Try(sys.env("TRAVIS_PULL_REQUEST")).getOrElse("false") != "false"
  val branch   = Try(sys.env("TRAVIS_BRANCH")).getOrElse("??")
  val snapshot = version.value.trim.endsWith("SNAPSHOT")
  val log = streams.value.log
  (travis, pr, branch, snapshot) match {
    case (true, false, "master", true) => publish
    case _ =>
      if (!travis) log.info("Not on travis, so not publishing")
      if (pr) log.info("PR build. not publishing snapshot")
      if (branch != "master") log.info("Not on master. not publishing snapshot")
      if (!snapshot) log.info("Not a snapshot build. Not publishing")
      Def.task ()
  }
}

publishMasterOnTravis := publishMasterOnTravisImpl.value

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.contains("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else                        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

credentials += Credentials(
  "Sonatype Nexus Repository Manager", "oss.sonatype.org",
  sys.env.get("SONATYPE_USERNAME").getOrElse(""),
  sys.env.get("SONATYPE_PASSWORD").getOrElse("")
)

pomExtra :=
<scm>
  <url>git@github.com:smootoo/freeslick.git</url>
  <connection>scm:git:git@github.com:smootoo/freeslick.git</connection>
</scm>
<developers>
   <developer>
      <id>fommil</id>
      <name>Sam Halliday</name>
   </developer>
   <developer>
      <id>szeiger</id>
      <name>Stefan Zeiger</name>
   </developer>
  <developer>
     <id>smootoo</id>
     <name>Sue Carter</name>
  </developer>
</developers>
