# sbt-pom-plugin

This [SBT] plugin allows one to extract project metadata from POM files. This
is useful for situations where one must maintain SBT and Maven builds in
parallel for the same project. Often one uses Maven on their automated build
server, and SBT for local development.

The plugin is very unsophisticated. If your build is complex and makes use of
myriad Maven plugins, you're not going to find the solution to your problems in
this plugin. However, if you just use your POM to specify project metadata and
enumerate your project dependencies, then you may just be in luck.

## Usage

There are two ways to use this plugin. In single module mode, and in
multi-module mode. For a single module project, you simply extract SBT
configuration information from your POM and stuff it into your SBT build in the
right place.

Regardless of which way you use it, you must create a `project/plugins.sbt`
file that contains the follow:

    addSbtPlugin("com.samskivert" % "sbt-pom-plugin" % "1.0-SNAPSHOT")

Note that this plugin is not published to a shared repository yet, so you need
to check out the code and `sbt publish-local` it (and you'll need to check out
[pom-util] and do the same before installing this plugin).

To use this for a single module project, do the following:

    seq(samskivert.POMPlugin.pomToSettings("pom.xml") :_*)

For a multi-module project, things are a bit trickier, but that is because the
plugin will automatically set up inter-project dependencies so that your
modules properly depend on one another. Here's a simple example:

    import samskivert.ProjectBuilder

    object FooBuild extends Build {
      val builder = new ProjectBuilder("pom.xml") {
        // optionally provide settings used by all module projects
        override val globalSettings = Seq(...any SBT-specific settings...)
        // optionally provide settings for individual module projects
        override def projectSettings (name :String) = name match {
          case "core" => Seq(...SBT-specific settings for core module...)
          case "server" => Seq(...SBT-specific settings for server module...)
          case _ => Nil // any other modules have no specific settings
        }
      }

      lazy val core = builder("core")
      lazy val server = builder("server")
      lazy val other = builder("other")
      lazy val foo = Project("foo", file(".")) aggregate(core, server, other)
    }

## Limitations

Currently the plugin will extract basic metadata (i.e. `groupId`, `artifactId`,
`version`) and dependencies. It currently only understands `compile` and `test`
dependencies.

It's not complicated to extend the internals of the plugin to handle more
metadata or to handle other dependency properties, I just haven't needed them
yet. Fork sbt-pom-plugin and/or [pom-util] and send a pull request if there's
something you need.

## License

sbt-pom-plugin is released under the New BSD License, which can be found in the
[LICENSE] file.

[SBT]: https://github.com/harrah/xsbt/wiki
[pom-util]: https://github.com/samskivert/pom-util
[LICENSE]: https://github.com/samskivert/sbt-pom-plugin/blob/master/LICENSE