//
// sbt-pom-plugin - Makes Maven project metadata usable from SBT
// http://github.com/samskivert/sbt-pom-plugin

package samskivert

import java.io.File

import org.junit.Test
import org.junit.Assert._

import pomutil.{POM, Dependency}

class POMUtilTest {

  @Test def testExpandModules () {
    val home = System.getProperty("user.home")
    POM.fromFile(new File(home + "/projects/playn/pom.xml")) foreach { pom =>
      val modules = POMUtil.resolveModules(pom)
      val fqMods = POMUtil.expandModules(modules, pom, _.modules)
      assertTrue(fqMods.find(_ contains("-")).isDefined)
      val javaFqMods = POMUtil.expandModules(modules, pom, _.modules("java"))
      assertTrue(fqMods.find(_ contains("-java")).isDefined)
      // println(POMUtil.expandModules(modules, pom, _.allModules))
    }
  }
}
