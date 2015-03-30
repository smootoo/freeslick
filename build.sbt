import sbt._
import ScoverageSbtPlugin.ScoverageKeys._

// NOTE: the following skips the MS-SQL tests
// it:test-only * -- -l MSSQL

organization := "com.github.fommil"

name := "freeslick"

scalaVersion := "2.10.5"

version := "2.0.3-SNAPSHOT"

//resolvers += Resolver.sonatypeRepo("snapshots")

configs(IntegrationTest)
inConfig(IntegrationTest)(Defaults.testSettings)
parallelExecution in IntegrationTest := false

libraryDependencies ++= Seq(
  "com.typesafe.slick"  %% "slick"                       % "2.0.3",
  "com.typesafe.slick"  %% "slick-testkit"               % "2.0.3"  % "test;it",
  "com.novocode"        %  "junit-interface"             % "0.10"   % "test;it",
  "org.scalatest"       %% "scalatest"                   % "2.2.4"  % "test;it",
  "org.scalamock"       %% "scalamock-scalatest-support" % "3.2.1"  % "test;it",
  "org.scalacheck"      %% "scalacheck"                  % "1.12.2" % "test;it",
  "ch.qos.logback"       % "logback-classic"             % "1.1.2"  % "test;it",
  // jTDS 2.3.x is JDK 1.7+ so stick with 1.2.x
  "net.sourceforge.jtds" % "jtds"                        % "1.2.8"  % "test;it"
)

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

javaOptions ++= Seq("-XX:MaxPermSize=256m", "-Xmx2g", "-XX:+UseConcMarkSweepGC")

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

homepage := Some(url("http://github.com/fommil/freeslick"))

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.contains("SNAP")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else                    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

credentials += Credentials(
  "Sonatype Nexus Repository Manager", "oss.sonatype.org",
  sys.env.get("SONATYPE_USERNAME").getOrElse(""),
  sys.env.get("SONATYPE_PASSWORD").getOrElse("")
)

pomExtra :=
<scm>
  <url>git@github.com:fommil/freeslick.git</url>
  <connection>scm:git:git@github.com:fommil/freeslick.git</connection>
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
</developers>
