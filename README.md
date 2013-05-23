# sbt-pom-util

This [SBT] library (it's not really a plugin) allows one to extract project metadata from POM
files. This is useful for situations where one must maintain SBT and Maven builds in parallel for
the same project. Often one uses Maven on their automated build server, and SBT for local
development.

The plugin is very unsophisticated. If your build is complex and makes use of myriad Maven plugins,
you're not going to find the solution to your problems in this plugin. However, if you just use
your POM to specify project metadata and enumerate your project dependencies, then you may just be
in luck.

## Requirements

This library was built against [SBT] 0.12.2 and Scala 2.9.2. However, it should work with newer
versions of Scala in the 2.9 series and maybe even newer versions of SBT in the 0.12 series. It
does not integrate tightly into SBT's internals, and thus I have not embedded an SBT (nor Scala)
version into the artifact name.

## Usage

There are two ways to use this plugin. In single module mode, and in multi-module mode. For a
single module project, you simply SBT configuration information from your POM and stuff it into
your SBT build in the right place.

Regardless of which way you use it, you must create a `project/plugins.sbt` file that contains the
follow:

    libraryDependencies += "com.samskivert" % "sbt-pom-util" % "0.4"

The library is published to Maven Central, so you will not need to add any custom resolvers.

To use this for a single module project, do the following:

    seq(samskivert.POMUtil.pomToSettings("pom.xml") :_*)

For a multi-module project, things are a bit trickier, but that is because the plugin will
automatically set up inter-project dependencies and a root module so that your modules properly
depend on one another. Here's a simple example:

    object FooBuild extends Build {
      val builder = new samskivert.ProjectBuilder("pom.xml") {
        // optionally provide settings used by all module projects
        override val globalSettings = Seq(...any SBT-specific settings...)

        // optionally provide settings for individual module projects
        override def projectSettings (name :String) = name match {
          case "core" => Seq(...SBT-specific settings for core module...)
          case "server" => Seq(...SBT-specific settings for server module...)
          case _ => Nil // any other modules have no specific settings
        }
      }

      // you can also optionally pass a profile name to projects and root to use all of the
      // sub-modules defined in the supplied profile
      override def projects = builder.projects
      lazy val root = builder.root
    }

## Limitations

Currently the plugin will extract:

  * basic metadata (i.e. `groupId`, `artifactId`, `version`)
  * Scala version (if defined as a `scala.version` property in your POM)
  * dependency information for `compile`, `provided` and `test` dependencies; it also groks scopes

It's not complicated to extend the internals of the plugin to handle more metadata or to handle
other dependency properties, I just haven't needed them yet. Fork sbt-pom-util and/or [pom-util]
and send a pull request if there's something you need.

## License

sbt-pom-util is released under the New BSD License, which can be found in the [LICENSE] file.

[SBT]: https://github.com/harrah/xsbt/wiki
[pom-util]: https://github.com/samskivert/pom-util
[LICENSE]: https://github.com/samskivert/sbt-pom-util/blob/master/LICENSE
