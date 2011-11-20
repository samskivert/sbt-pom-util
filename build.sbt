sbtPlugin := true

organization := "com.samskivert"

name := "sbt-pom-plugin"

version := "1.0-SNAPSHOT"

scalaVersion := "2.9.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies += "com.samskivert" % "pom-util" % "1.0-SNAPSHOT"
