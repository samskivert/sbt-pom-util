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
 * val builder = new ProjectBuilder("pom.xml") {
 *   override def globalSettings = Seq(...)
 *   override def projectSettings (name :String) = name match { .. }
 * }
 * lazy val core = builder("core")
 * lazy val tools = builder("tools")
 * lazy val client = builder("client")
 * }}}
 * The stock SBT settings will be automatically included.
 */
class ProjectBuilder (path :String)
{
  /** Defines extra settings that are applied to all projects. */
  def globalSettings :Seq[Setting[_]] = Nil

  /** Defines extra settings that are to the project named `name`. */
  def projectSettings (name :String) :Seq[Setting[_]] = Nil

  /** Creates an SBT project for the specified sub-module. */
  def apply (name :String) :Project = _projects.getOrElseUpdate(name, {
    val (pom, pomFile) = _modules.getOrElse(name, sys.error("No sub-module POM in " + name + "."))

    val (sibdeps, odeps) = pom.depends.partition(isSibling)
    val psettings = Defaults.defaultSettings ++ POMUtil.pomToSettings(pom) ++ globalSettings ++
        projectSettings(name) ++ Seq(
          libraryDependencies ++= odeps.map(POMUtil.toIvyDepend)
    )
    val proj = Project(name, pomFile.getParentFile, settings = psettings)
    // finally apply all of the sibling dependencies
    (proj /: sibdeps.map(_.id).map(_depToModule))((p, dname) => p dependsOn apply(dname))
  })

  private def resolveSubPOM (prefix :List[String])(name :String) :Seq[(String,(POM,File))] = {
    val pathComps = ("pom.xml" :: name :: prefix) reverse
    val pomFile = pathComps.tail.foldLeft(new File(pathComps.head))(new File(_, _))
    POM.fromFile(pomFile) match {
      case None => System.err.println("Failed to read sub-module POM: " + pomFile) ; List()
      case Some(p) => {
        val pomId = (name :: prefix).reverse.mkString("-")
        (pomId, (p, pomFile)) +: p.modules.flatMap(resolveSubPOM(name :: prefix))
      }
    }
  }

  private def isSibling (depend :Dependency) = _depToModule.contains(depend.id)

  private val _pom = POM.fromFile(new File(path)).getOrElse(
    sys.error("Unable to load POM from " + path))
  private val (_modules, _depToModule) = {
    val data = _pom.modules.flatMap(resolveSubPOM(List()))
    (data.toMap, data.map(t => (t._2._1.id, t._1)).toMap)
  }
  private val _projects = scala.collection.mutable.Map[String,Project]()
}
