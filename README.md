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

This library was built against [SBT] 0.13 and Scala 2.10. It does not integrate tightly into SBT's
internals, and thus I have not embedded an SBT version into the artifact name.

## Usage

There are two ways to use this plugin. In single module mode, and in multi-module mode. For a
single module project, you simply SBT configuration information from your POM and stuff it into
your SBT build in the right place.

Regardless of which way you use it, you must create a `project/plugins.sbt` file that contains the
follow:

    libraryDependencies += "com.samskivert" % "sbt-pom-util" % "0.6"

The library is published to Maven Central, so you will not need to add any custom resolvers.

### Single module project

To use this for a single module project, do the following:

    seq(samskivert.POMUtil.pomToSettings("pom.xml") :_*)

### Multi-module project

For a multi-module project, the plugin will automatically set up inter-project dependencies and a
root module so that your modules properly depend on one another. You'll have to use a `.scala`
build file to make this work, but otherwise you just extend `MavenBuild` and everything is taken
care of:

    object FooBuild extends MavenBuild {
      // optionally provide settings used by all module projects
      override val globalSettings = Seq(...any SBT-specific settings...)

      // optionally provide settings for individual module projects
      override def projectSettings (name :String) = name match {
        case "core" => Seq(...SBT-specific settings for core module...)
        case "server" => Seq(...SBT-specific settings for server module...)
        case _ => Nil // any other modules have no specific settings
      }

      // optionally specify which Maven profiles to consider "active"
      override def profiles :Seq[String] = Seq()
    }

### Workspace projects

If you use the `MavenBuild` approach above, you can also create a `.workspace` file which specifies
the path to other `sbt-pom-util`-enabled projects which should be integrated directly into your SBT
build. The `.workspace` file just contains a list of paths (relative or absolute) to the other
projects.

This will cause any dependencies in your main project to resolve to modules in the workspace
projects if their versions match (rather than resolving them to the artifact in the local Ivy
repository).

For example, you might have a directory structure like so:

{{{
/home/projects/foolib   # builds foocorp:foolib:1.3-SNAPSHOT
/home/projects/barlib   # builds barcorp:barlib:1.1-SNAPSHOT
/home/projects/magicapp # your main project
}}}

You can create a `.workspace` file in the `magicapp` directory with the following contents:

{{{
../foolib
../barlib
}}}

Assuming `magicapp/pom.xml` contains dependencies on `foocorp:foolib:1.3-SNAPSHOT` and
`barcorp:barlib:1.1-SNAPSHOT`, then those projects will be loaded directly into your SBT session
when running SBT on `magicapp`. Rebuilding `magicapp` will automatically rebuild changed files in
`foolib` or `barlib`.

This is akin to having both projects in, for example, your Eclipse workspace, hence the name.

Note also that if `foolib` or `barlib` also contains a `.workspace` file, that will be transitively
applied and you will end up with yet more projects wired directly into your SBT session.

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
