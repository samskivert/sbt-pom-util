//
// sbt-pom-plugin - Makes Maven project metadata usable from SBT
// http://github.com/samskivert/sbt-pom-plugin

package samskivert

import java.io.File
import scala.collection.mutable.ListBuffer

import sbt._
import sbt.Keys._

import pomutil.{POM, Dependency}

/**
 * Makes Maven project metadata usable from SBT.
 */
object POMUtil extends Plugin {

  /** Reads SBT settings from the POM file at the specified path. */
  def pomToSettings (path :String) :Seq[Setting[_]] = pomToSettings(new File(path))

  /** Reads SBT settings from the supplied POM file. */
  def pomToSettings (file :File) :Seq[Setting[_]] =
    pomToSettings(POM.fromFile(file).getOrElse(sys.error("Invalid POM file: " + file)))

  /** Reads SBT settings from the supplied POM.
   * @param excludeDepends if true, dependency configuration omitted from settings. */
  def pomToSettings (pom :POM, excludeDepends :Boolean = false) :Seq[Setting[_]] = {
    val meta = ListBuffer[Setting[_]](
      organization := pom.groupId,
      name := pom.artifactId,
      version := pom.version
    )
    // if the POM defines a scala.version property, use it
    pom.getAttr("scala.version") foreach { v => meta += (scalaVersion := v) }
    // if we're not excluding dependencies, tack those on
    if (!excludeDepends) meta += (libraryDependencies ++= pom.depends.map(toIvyDepend))
    // finally convert back to the blissful world of immutability
    meta.toSeq
  }

  /** Resolves all modules (in all profiles) using the supplied POM as the root. Loads and resolves
    * the POMs for all modules and recurses until the entire tree rooted at `pom` is resolved.
    * @return a map from fully-qualified module name (e.g. grandparent-parent-child) to the module's
    * parsed POM and POM file.
    */
  def resolveModules (pom :POM) :Map[String,(POM,File)] = {
    val root = pom.file.get.getParentFile
    def resolveSubPOM (prefix :List[String])(name :String) :Seq[(String,(POM,File))] = {
      val pathComps = ("pom.xml" :: name :: prefix) reverse
      val pomFile = pathComps.foldLeft(root)(new File(_, _))
      POM.fromFile(pomFile) match {
        case None => System.err.println("Failed to read sub-module POM: " + pomFile) ; List()
        case Some(p) => {
          val pomId = (name :: prefix).reverse.mkString("-")
          (pomId, (p, pomFile)) +: p.allModules.flatMap(resolveSubPOM(name :: prefix))
        }
      }
    }
    pom.allModules.flatMap(resolveSubPOM(List())).toMap
  }

  /** Returns a list of all fully qualified modules in the supplied multi-module project tree. `mf`
    * is used to expand the modules of a given POM and can use a desired set of profiles. */
  def expandModules (resolved :Map[String,(POM,File)], pom :POM,
                     mf :POM => Seq[String]) :Seq[String] = {
    def expand (fqMod :String) :Seq[String] = {
      val pom = resolved(fqMod)._1
      if (pom.packaging.toLowerCase == "pom") mf(pom).flatMap(m => expand(fqMod + "-" + m))
      else Seq(fqMod)
    }
    mf(pom).flatMap(expand)
  }

  /** Converts a Maven dependency to an Ivy dependency. */
  def toIvyDepend (depend :Dependency) = {
    // TODO: handle type, etc.
    val bare = depend.groupId % depend.artifactId % depend.version
    depend.classifier match {
      // TODO: none of these seem to do quite the right thing re classifier=sources
      case Some("sources") => bare % "compile->sources"
      case Some(cfier)     => bare classifier cfier // TODO: this is probably wonky
      case None            => depend.scope match {
        case "test"     => bare % "test"
        case "provided" => bare % "provided"
        // TODO: other scopes?
        case _ => bare
      }
    }
  }
}
