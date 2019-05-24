package ru.d10xa.jadd.show

import ru.d10xa.jadd.testkit.TestBase
import ru.d10xa.jadd.ProjectFileReaderMemory
import ru.d10xa.jadd.Scope.Test

class SbtShowCommandTest extends TestBase {

  val emptyProjectFileReader = new ProjectFileReaderMemory(Map.empty)

  test("seq") {
    val source =
      s"""
         |libraryDependencies ++= Seq(
         |  "ch.qos.logback" % "logback-classic" % "1.2.3",
         |  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
         |  "io.circe" %% "circe-parser" % "0.9.3",
         |  "io.circe" %% "circe-generic" % "0.9.3",
         |  "org.jline" % "jline" % "3.7.1"
         |)
       """.stripMargin

    val result = new SbtShowCommand(source, emptyProjectFileReader).show()
    val expected = Seq(
      art("ch.qos.logback:logback-classic:1.2.3"),
      art("com.typesafe.scala-logging:scala-logging%%:3.9.0").scala2_12,
      art("io.circe:circe-parser%%:0.9.3").scala2_12,
      art("io.circe:circe-generic%%:0.9.3").scala2_12,
      art("org.jline:jline:3.7.1")
    )

    result shouldEqual expected
  }

  test("seq and single") {
    val source =
      s"""
         |libraryDependencies ++= Seq(
         |  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
         |)
         |libraryDependencies += "org.typelevel" %% "cats-core" % "1.1.0"
       """.stripMargin

    val result = new SbtShowCommand(source, emptyProjectFileReader).show()
    val expected = Seq(
      art("com.typesafe.scala-logging:scala-logging%%:3.9.0").scala2_12,
      art("org.typelevel:cats-core%%:1.1.0").scala2_12
    )
    result shouldEqual expected
  }

  test("with scope test") {
    val source =
      s"""
         |libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
         |libraryDependencies ++= Vector("junit" % "junit" % "4.12" % Test)
         |libraryDependencies += "a" % "b" % "1" % UnknownScopeShouldBeIgnored
       """.stripMargin

    val result = new SbtShowCommand(source, emptyProjectFileReader)
      .show()
      .sortBy(_.artifactId)
    val expected = Seq(
      art("org.scalatest:scalatest_2.12:3.0.5")
        .copy(scope = Some(Test))
        .scala2_12,
      art("junit:junit:4.12")
        .copy(scope = Some(Test)),
      art("a:b:1")
    ).sortBy(_.groupId)
    result shouldEqual expected
  }

  test("Dependencies import") {
    val source =
      s"""
         |import Dependencies._
         |libraryDependencies ++= Seq(
         |  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
         |)
       """.stripMargin

    val dependenciesFile =
      """
        |import sbt._
        |object Dependencies {
        |  lazy val junit = "junit" % "junit" % "4.12"
        |  lazy val catsCore = "org.typelevel" %% "cats-core" % "1.1.0"
        |}""".stripMargin
    val result = new SbtShowCommand(
      source,
      new ProjectFileReaderMemory(
        Map("project/Dependencies.scala" ->
          dependenciesFile))
    ).show()

    val expected = Seq(
      art("com.typesafe.scala-logging:scala-logging%%:3.9.0").scala2_12,
      art("junit:junit:4.12"),
      art("org.typelevel:cats-core%%:1.1.0").scala2_12
    )
    result shouldEqual expected
  }

}
