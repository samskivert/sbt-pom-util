//
// sbt-pom-plugin - Makes Maven project metadata usable from SBT
// http://github.com/samskivert/sbt-pom-plugin

package samskivert

import java.io.File
import scala.io.Source

import sbt._
import sbt.Keys._

import pomutil.{POM, Dependency}

/** Now an implementation detail, see [MavenBuild] for multi-module projects. */
class ProjectBuilder (rootPom :File) {

  /** Defines extra settings that are applied to all projects. */
  def globalSettings :Seq[Setting[_]] = Nil

  /** Defines extra settings for the module named `name`.
   * @param pom the POM for the module in question. */
  def moduleSettings (name :String, pom :POM) :Seq[Setting[_]] = Nil

  /** Returns SBT projects for sub-modules in this project.
    * @param profiles the Maven profiles to consider "active" when enumerating sub-modules.
    * Pass `Seq("*")` to consider all profiles active. */
  def projects (profiles :Seq[String]) :Seq[Project] = {
    val modules = profiles match {
      case Seq()    => POMUtil.expandModules(_modules, _pom, _.modules)
      case Seq("*") => POMUtil.expandModules(_modules, _pom, _.allModules)
      case ps       => POMUtil.expandModules(_modules, _pom, p => ps.flatMap(p.modules).distinct)
    }
    modules.map(apply) :+ root(modules)
  }

  /** Creates an SBT project for the specified sub-module. You probably want to use `root` +
    * `projects` and have projects automatically created, but this is here for special needs. */
  def apply (name :String) :Project = {
    val (pom, pomFile) = _modules.getOrElse(name, sys.error("No sub-module POM in " + name + "."))
    val (locdeps, odeps) = pom.depends.partition(isLocal)
    val msettings = baseSettings(pom) ++ moduleSettings(name, pom) ++ Seq(
      libraryDependencies ++= odeps.map(POMUtil.toIvyDepend)
    )
    // create the project, and apply all of the sibling dependencies
    (Project(name, pomFile.getParentFile, settings = msettings) /: locdeps) {
      case (p, dep) => p dependsOn(projectRef(dep) % (dep.scope + "->compile"))
    }
  }

  /** Returns true if the specified project is managed by this builder. */
  private def isLocal (depend :Dependency) :Boolean =
    _depToModule.contains(depend.id) || _builders.exists(_.isLocal(depend))

  /** Returns the SBT project reference for the supplied POM dependency.
    * Requires that `depend` has already been confirmed as local by `isLocal`. */
  private def projectRef (depend :Dependency) :ProjectReference =
    if (_depToModule.contains(depend.id)) ProjectRef(_root.toURI, _depToModule(depend.id))
    else _builders.find(_.isLocal(depend)).map(_.projectRef(depend)).get

  /** Creates a root project with settings from the top-level POM which aggregates `modules`. */
  private def root (modules :Seq[String]) :Project = Project(
    _pom.artifactId, _root, settings = baseSettings(_pom)).
    aggregate(modules.map(LocalProject) :_*)

  private def baseSettings (pom :POM) =
    Defaults.defaultSettings ++ POMUtil.pomToSettings(pom, true) ++ globalSettings

  private val _root = rootPom.getParentFile match {
    case null => file(".")
    case f => f
  }
  private val _pom = POM.fromFile(rootPom).getOrElse(sys.error("Unable to load POM from " + rootPom))
  private val _modules :Map[String, (POM,File)] = POMUtil.resolveModules(_pom)
  private val _depToModule :Map[String,String] = _modules.map(t => (t._2._1.id, t._1))

  private val _builders :Seq[ProjectBuilder] = try {
    val workFile = new File(_root, ".workspace")
    if (!workFile.exists) Seq()
    else Source.fromFile(workFile).getLines.toSeq.map(p => new File(p)).collect {
      case root if (root.isDirectory) => new File(root, "pom.xml")
      case root if (root.isFile) => root
    }.map(root => new ProjectBuilder(root))
  } catch {
    case e :Throwable => e.printStackTrace(System.err) ; Seq()
  }
}
