//
// sbt-pom-plugin - Makes Maven project metadata usable from SBT
// http://github.com/samskivert/sbt-pom-plugin

package samskivert

import sbt._
import pomutil.POM

/** Handles multi-module projects. Sub-module interdependencies will automatically be wired up as
  * SBT project dependencies. Other Maven dependencies will be mapped to Ivy dependencies. See
  * project README for usage example.
  */
class MavenBuild (pomFile :String) extends Build {

  def this () = this("pom.xml")

  /** Defines extra settings that are applied to all modules. */
  def globalSettings :Seq[Setting[_]] = Nil

  /** Defines extra settings for the module named `name`.
    * @param pom the POM for the module in question. */
  def moduleSettings (name :String, pom :POM) :Seq[Setting[_]] = Nil

  /** Returns the profiles to consider "active" when turning modules into projects.
    * Return `Seq("*")` to ignore profiles and activate all modules in all profiles. */
  def profiles :Seq[String] = Seq()

  override def projectDefinitions (base :File) = projects(
    new ProjectBuilder(new File(base, pomFile)) {
      override def globalSettings = MavenBuild.this.globalSettings
      override def moduleSettings (name :String, pom :POM) =
        MavenBuild.this.moduleSettings(name, pom)
    })

  /** Returns the projects (modules in Maven parlance) used by this build. Default impl asks
    * `builder` for all modules in the profiles defined by [profiles]. */
  protected def projects (builder :ProjectBuilder) = builder.projects(profiles)
}
