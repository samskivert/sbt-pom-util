//
// $Id$

package samskivert

import java.io.File

import sbt._
import sbt.Keys._

import pomutil.{POM, Dependency}

/**
 * Extracts SBT settings from Maven POM files.
 */
object POMSettings extends Plugin
{
  def pomToSettings (path :String) :Seq[Setting[_]] = pomToSettings(new File(path))

  def pomToSettings (file :File) :Seq[Setting[_]] = {
    val pom = POM.fromFile(file).getOrElse(sys.error("Unable to read POM " + file))
    Seq(
      organization := pom.groupId,
      name := pom.artifactId,
      version := pom.version,
      libraryDependencies ++= pom.depends.map(toIvyDepend)
    )
  }

  def toIvyDepend (depend :Dependency) = {
    // TODO: handle type, classifier, scope, etc.
    depend.groupId % depend.artifactId % depend.version
  }
}
