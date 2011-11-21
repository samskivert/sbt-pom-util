//
// sbt-pom-plugin - Makes Maven project metadata usable from SBT
// http://github.com/samskivert/sbt-pom-plugin

package samskivert

import java.io.File

import sbt._
import sbt.Keys._

import pomutil.{POM, Dependency}

/**
 * Handles multimodule projects. Sub-module interdependencies will automatically be wired up as SBT
 * project dependencies. Other Maven dependencies will be mapped to Ivy dependencies.
 *
 * Instantiate a project builder with the top-level POM. Use it to create SBT sub-projects like so:
 * {{{
 * val builder = new ProjectBuilder("pom.xml")
 * lazy val core = builder("core", extraSettings)
 * lazy val tools = builder("tools", extraSettings)
 * lazy val client = builder("client", extraSettings)
 * }}}
 * The stock SBT settings will be automatically included, and `extraSettings` is optional.
 */
class ProjectBuilder (path :String)
{
  /** Creates an SBT project for the specified sub-module. */
  def apply (name :String, settings :Seq[Setting[_]]) :Project = {
    val pom = _modules.getOrElse(name, sys.error("No sub-module POM in " + name + "."))

    // extract any sibling dependencies and turn them into project dependencies
    sys.error("TODO")
  }

  private def resolveSubPOM (name :String) = {
    val pomFile = new File(new File(name), "pom.xml")
    POM.fromFile(pomFile) map(p => Some(name -> p)) getOrElse {
      System.err.println("Failed to read sub-module POM: " + pomFile)
      None
    }
  }

  private val _pom = POM.fromFile(new File(path)).getOrElse(
    sys.error("Unable to load POM from " + path))
  private val _modules = _pom.modules.flatMap(resolveSubPOM).toMap
}
