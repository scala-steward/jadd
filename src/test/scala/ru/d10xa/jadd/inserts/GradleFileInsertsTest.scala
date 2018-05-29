package ru.d10xa.jadd.inserts

import org.scalatest.FunSuiteLike
import org.scalatest.Matchers
import ru.d10xa.jadd.Artifact

class GradleFileInsertsTest extends FunSuiteLike with Matchers {

  test("4 spaces insert") {
    def buildFileSource =
      """|dependencies {
         |    testCompile 't:t:1.0'
         |}
         |""".stripMargin

    val result = new GradleFileInserts().appendAll(
      buildFileSource,
      Seq(Artifact("a:b:2.0"), Artifact("x:y:3.0"))
    )

    result shouldEqual
      """|dependencies {
         |    testCompile 't:t:1.0'
         |    compile "a:b:2.0"
         |    compile "x:y:3.0"
         |}
         |""".stripMargin
  }

  test("tab insert") {
    val content = StringContext.treatEscapes(
      """apply plugin: 'java'
        |repositories {
        |\tjcenter()
        |}
        |dependencies {
        |\ttestCompile 'junit:junit:4.12'
        |}
        |""".stripMargin
    )

    val newContent = new GradleFileInserts()
      .appendAll(content, Seq(Artifact("org.codehaus.groovy:groovy-all:2.4.15")))

    newContent shouldEqual
      StringContext.treatEscapes(
        """apply plugin: 'java'
          |repositories {
          |\tjcenter()
          |}
          |dependencies {
          |\ttestCompile 'junit:junit:4.12'
          |\tcompile "org.codehaus.groovy:groovy-all:2.4.15"
          |}
          |""".stripMargin
      )
  }

  test("simple update") {
    def buildFileSource =
      """|dependencies {
         |    compile 'u:u:1.0'
         |}
         |""".stripMargin

    val result = new GradleFileInserts().appendAll(
      buildFileSource,
      Seq(Artifact("u:u:2.0"))
    )

    result shouldEqual
      """|dependencies {
         |    compile "u:u:2.0"
         |}
         |""".stripMargin
  }

  test("add dependencies block if absent") {
    def buildFileSource =
      """|repositories {
         |    jcenter()
         |}""".stripMargin

    val result = new GradleFileInserts().appendAll(
      buildFileSource,
      Seq(Artifact("a:a:1.0"), Artifact("b:b:2.0"))
    )

    result shouldEqual
      """|repositories {
         |    jcenter()
         |}
         |dependencies {
         |    compile "a:a:1.0"
         |    compile "b:b:2.0"
         |}
         |""".stripMargin
  }

}
