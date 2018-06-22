package ru.d10xa.jadd.it

import org.scalatest.FunSuiteLike
import org.scalatest.Matchers
import ru.d10xa.jadd.Jadd
import ru.d10xa.jadd.testkit.BuildFileTestBase

class MainSbtTest extends BuildFileTestBase("build.sbt") with FunSuiteLike with Matchers {

  test("update dependency"){
    write(
      """
        |libraryDependencies += "ch.qos.logback" % "logback-core" % "1.0.0"
      """.stripMargin)

    Jadd.main(Array("install", "-q", projectDirArg, "ch.qos.logback:logback-core"))

    read() shouldEqual
      """
        |libraryDependencies += "ch.qos.logback" % "logback-core" % "1.2.3"
      """.stripMargin
  }

}
