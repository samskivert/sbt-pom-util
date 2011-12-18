organization := "com.samskivert"

name := "sbt-pom-plugin"

version := "0.1-SNAPSHOT"

scalaVersion := "2.9.1"

crossPaths := false

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "org.scala-tools.sbt" %% "sbt" % "0.11.2",
  "com.samskivert" % "pom-util" % "0.1"
)

publishMavenStyle := true

publishTo <<= (version) { v: String =>
  val root = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at root + "content/repositories/snapshots/")
  else Some("staging" at root + "service/local/staging/deploy/maven2")
}

credentials += Credentials(Path.userHome / ".ivy2" / "credentials-sonatype")
