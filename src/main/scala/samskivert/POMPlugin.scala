//
// sbt-pom-plugin - Makes Maven project metadata usable from SBT
// http://github.com/samskivert/sbt-pom-plugin

package samskivert

import java.io.File

import sbt._
import sbt.Keys._

import pomutil.{POM, Dependency}

/**
 * Makes Maven project metadata usable from SBT.
 */
object POMPlugin extends Plugin
{
  /** Reads SBT settings from the POM file at the specified path. */
  def pomToSettings (path :String) :Seq[Setting[_]] = pomToSettings(new File(path))

  /** Reads SBT settings from the supplied POM file. */
  def pomToSettings (file :File) :Seq[Setting[_]] =
    pomToSettings(POM.fromFile(file).getOrElse(sys.error("Invalid POM file: " + file)))

  /** Reads SBT settings from the supplied POM.
   * @param excludeDepends if true, dependency configuration omitted from settings. */
  def pomToSettings (pom :POM, excludeDepends :Boolean = false) :Seq[Setting[_]] = {
    val meta = Seq(
      organization := pom.groupId,
      name := pom.artifactId,
      version := pom.version
      // TODO: add support for various standard build settings
    )
    if (excludeDepends) meta
    else meta ++ Seq(libraryDependencies ++= pom.depends.map(toIvyDepend))
  }

  /** Converts a Maven dependency to an Ivy dependency. */
  def toIvyDepend (depend :Dependency) = {
    // TODO: handle type, classifier, scope, etc.
    val bare = depend.groupId % depend.artifactId % depend.version
    if (depend.scope == "test") bare % "test" else bare
  }
}
