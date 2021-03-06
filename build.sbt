organization := "com.samskivert"

name := "sbt-pom-util"

version := "0.7-SNAPSHOT"

scalaVersion := "2.10.3"

crossPaths := false

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "org.scala-sbt" % "sbt" % "0.13.0" % "provided",
  "com.samskivert" %% "pom-util" % "0.6",
  "junit" % "junit" % "4.10" % "test",
  "com.novocode" % "junit-interface" % "0.7" % "test->default"
)

//
// Various POM additions and Maven Central publishing bits

publishMavenStyle := true

// don't add the scala-tools repository to our POM
pomIncludeRepository := { (repo: MavenRepository) => false }

publishTo <<= (version) { v: String =>
  val root = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at root + "content/repositories/snapshots/")
  else Some("staging" at root + "service/local/staging/deploy/maven2")
}

credentials += Credentials(Path.userHome / ".ivy2" / "credentials-sonatype")

description := "SBT utility code that extracts metadata from POM files."

homepage := Some(url("http://github.com/samskivert/sbt-pom-util"))

licenses += ("The (New) BSD License" -> url("http://www.opensource.org/licenses/bsd-license.php"))

startYear := Some(2011)

pomExtra :=
  <developers>
    <developer>
      <id>samskivert</id>
      <name>Michael Bayne</name>
      <email>mdb@samskivert.com</email>
    </developer>
  </developers>
  <scm>
    <connection>scm:git:git://github.com/samskivert/sbt-pom-util.git</connection>
    <url>http://github.com/samskivert/sbt-pom-util</url>
  </scm>
