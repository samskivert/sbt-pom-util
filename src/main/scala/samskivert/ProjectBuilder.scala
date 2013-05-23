//
// sbt-pom-plugin - Makes Maven project metadata usable from SBT
// http://github.com/samskivert/sbt-pom-plugin

package samskivert

import java.io.File

import sbt._
import sbt.Keys._

import pomutil.{POM, Dependency}

/** Handles multimodule projects. Sub-module interdependencies will automatically be wired up as
  * SBT project dependencies. Other Maven dependencies will be mapped to Ivy dependencies.
  *
  * Instantiate ProjectBuilder with the top-level POM:
  * {{{
  * val builder = new ProjectBuilder("pom.xml") {
  *   override def globalSettings = Seq(...)
  *   override def projectSettings (name :String, pom :POM) = name match { .. }
  * }
  * override def projects = builder.projects // learn SBT about all the projects
  * val root = builder.root // aggregates all submodules into a root project
  * }}}
  *
  * The stock SBT settings will be automatically included.
  */
class ProjectBuilder (path :String)
{
  /** Defines extra settings that are applied to all projects. */
  def globalSettings :Seq[Setting[_]] = Nil

  /** Defines extra settings for the module named `name`.
   * @param pom the POM for the module in question. */
  def projectSettings (name :String, pom :POM) :Seq[Setting[_]] = Nil

  /** Returns SBT projects for sub-modules in the default profile of our top-level POM. */
  def projects :Seq[Project] = projects(_pom.modules)

  /** Returns SBT projects for all sub-modules defined by the specified profile. */
  def projects (profile :String) :Seq[Project] = projects(_pom.modules(profile))

  /** Returns SBT projects for the specified sub-modules. */
  def projects (modules :Seq[String]) :Seq[Project] = modules.map(apply)

  /** Returns SBT projects for all sub-modules in all profiles in our POM. */
  def projectsAll :Seq[Project] = projects(_pom.allModules)

  /** Creates a root project which aggregates the modules in the default profile. */
  def root :Project = root(_pom.modules)

  /** Creates a root project which aggregates all the projects in the specified profile. */
  def root (profile :String) :Project = root(_pom.modules(profile))

  /** Creates a root project with settings from the top-level POM which aggregates the specified
    * submodules. */
  def root (modules :Seq[String]) :Project = Project(
    _pom.artifactId, file("."), settings = baseSettings(_pom)).
    aggregate(modules.map(LocalProject) :_*)

  /** Creates a root project which aggregates all modules in all profiles. */
  def rootAll :Project = root(_pom.modules)

  /** Creates an SBT project for the specified sub-module. You probably want to use `root` +
    * `projects` and have projects automatically created, but this is here for special needs. */
  def apply (name :String) :Project = {
    val (pom, pomFile) = _modules.getOrElse(name, sys.error("No sub-module POM in " + name + "."))

    val (sibdeps, odeps) = pom.depends.partition(isSibling)
    val psettings = baseSettings(pom) ++ projectSettings(name, pom) ++ Seq(
      libraryDependencies ++= odeps.map(POMUtil.toIvyDepend)
    )
    val proj = Project(name, pomFile.getParentFile, settings = psettings)
    // finally apply all of the sibling dependencies
    (proj /: sibdeps) {
      case (p, dep) => p dependsOn(LocalProject(_depToModule(dep.id)) % (dep.scope + "->compile"))
    }
  }

  private def baseSettings (pom :POM) =
    Defaults.defaultSettings ++ POMUtil.pomToSettings(pom, true) ++ globalSettings

  private def resolveSubPOM (prefix :List[String])(name :String) :Seq[(String,(POM,File))] = {
    val pathComps = ("pom.xml" :: name :: prefix) reverse
    val pomFile = pathComps.tail.foldLeft(new File(pathComps.head))(new File(_, _))
    POM.fromFile(pomFile) match {
      case None => System.err.println("Failed to read sub-module POM: " + pomFile) ; List()
      case Some(p) => {
        val pomId = (name :: prefix).reverse.mkString("-")
        (pomId, (p, pomFile)) +: p.allModules.flatMap(resolveSubPOM(name :: prefix))
      }
    }
  }

  private def isSibling (depend :Dependency) = _depToModule.contains(depend.id)

  private val _pom = POM.fromFile(new File(path)).getOrElse(
    sys.error("Unable to load POM from " + path))
  private val _modules :Map[String, (POM,File)] = _pom.allModules.flatMap(
    resolveSubPOM(List())).toMap
  private val _depToModule :Map[String,String] = _modules.map(t => (t._2._1.id, t._1))
}
