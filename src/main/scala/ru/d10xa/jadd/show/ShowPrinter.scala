package ru.d10xa.jadd.show

import ru.d10xa.jadd.Artifact
import ru.d10xa.jadd.Indent
import ru.d10xa.jadd.Scope.Test
import ru.d10xa.jadd.show.GradleLang.Kotlin

sealed trait ShowPrinter {
  def single(a: Artifact): String
  def mkString(artifacts: List[Artifact]): String =
    artifacts.map(single).mkString("\n")
}

object JaddFormatShowPrinter extends ShowPrinter {
  def single(a: Artifact): String = {
    val artifactId: String =
      (a.needScalaVersionResolving, a.maybeScalaVersion) match {
        case (true, Some(scalaVersion)) =>
          a.artifactIdWithScalaVersion(scalaVersion)
        case _ => a.artifactId
      }
    a.maybeVersion match {
      case Some(v) => s"${a.groupId}:$artifactId:$v"
      case None => s"${a.groupId}:$artifactId"
    }
  }
}

object AmmoniteFormatShowPrinter extends ShowPrinter {

  def moduleId(a: Artifact): String = {
    val artifactId: String =
      (a.needScalaVersionResolving, a.maybeScalaVersion) match {
        case (true, Some(_)) =>
          s":${a.artifactIdWithoutScalaVersion}"
        case _ => a.artifactId
      }
    a.maybeVersion match {
      case Some(v) => s"${a.groupId}:$artifactId:$v"
      case None => s"${a.groupId}:$artifactId"
    }
  }

  def single(a: Artifact): String =
    s"import $$ivy.`${moduleId(a)}`"
}

object MillFormatShowPrinter extends ShowPrinter {
  def single(a: Artifact): String =
    s"""ivy"${AmmoniteFormatShowPrinter.moduleId(a)}""""

  override def mkString(artifacts: List[Artifact]): String =
    artifacts.map(single).mkString(",\n")
}

object GroovyFormatShowPrinter extends ShowPrinter {
  def single(a: Artifact): String = {
    val artifactId: String =
      (a.needScalaVersionResolving, a.maybeScalaVersion) match {
        case (true, Some(scalaVersion)) =>
          a.artifactIdWithScalaVersion(scalaVersion)
        case _ => a.artifactId
      }
    val versionKeyValue = a.maybeVersion match {
      case Some(version) => s", version = '$version'"
      case None => ""
    }
    s"@Grab(group='${a.groupId}', module='$artifactId'$versionKeyValue)"
  }
}

object LeiningenFormatShowPrinter extends ShowPrinter {
  def single(a: Artifact): String = {
    val artifactId: String =
      (a.needScalaVersionResolving, a.maybeScalaVersion) match {
        case (true, Some(scalaVersion)) =>
          a.artifactIdWithScalaVersion(scalaVersion)
        case _ => a.artifactId
      }
    val version = a.maybeVersion.getOrElse("LATEST")
    val moduleId = (a.groupId, artifactId) match {
      case (gId, aId) if gId == aId => aId
      case (gId, aId) => s"$gId/$aId"
    }
    s"""[$moduleId "$version"]"""
  }
}

sealed trait GradleLang

object GradleLang {
  object Groovy extends GradleLang
  object Kotlin extends GradleLang
}

final class GradleFormatShowPrinter(lang: GradleLang) extends ShowPrinter {
  def single(a: Artifact): String = {
    val artifactId: String =
      (a.needScalaVersionResolving, a.maybeScalaVersion) match {
        case (true, Some(scalaVersion)) =>
          a.artifactIdWithScalaVersion(scalaVersion)
        case _ => a.artifactId
      }
    val q = if (a.doubleQuotes) "\"" else "'"

    val configuration =
      a.configuration.getOrElse(
        a.scope
          .collect { case Test => "testImplementation" }
          .getOrElse("implementation"))

    val moduleId =
      (Some(a.groupId) :: Some(artifactId) :: a.maybeVersion :: Nil).flatten
        .mkString(":")

    val moduleIdWithOptionalParentheses =
      if (lang == Kotlin) s"($q$moduleId$q)" else s"$q$moduleId$q"
    s"$configuration $moduleIdWithOptionalParentheses"
  }
}

object MavenFormatShowPrinter extends ShowPrinter {

  def tag(tagName: String)(content: String): String =
    s"<$tagName>$content</$tagName>"
  val groupIdTag: String => String = tag("groupId")
  val artifactIdTag: String => String = tag("artifactId")
  val versionTag: String => String = tag("version")
  val scopeTag: String => String = tag("scope")

  def singleWithIndent(artifact: Artifact, indent: Indent): String = {
    val indentString: String = indent.take(1)

    def nl(str: String): String = s"\n$indentString$str"

    def artifactToString(artifact: Artifact): String = {
      val requiredGroupId = nl(groupIdTag(artifact.groupId))
      val requiredArtifactId = nl(artifactIdTag(artifact.artifactId))
      val optionalVersion: String =
        artifact.maybeVersion.fold("")(v => nl(versionTag(v)))
      val optionalScope: String = artifact.scope match {
        case Some(Test) => nl(scopeTag("test"))
        case None => ""
      }

      s"""<dependency>$requiredGroupId$requiredArtifactId$optionalVersion$optionalScope
         |</dependency>""".stripMargin
    }

    artifactToString(artifact.inlineScalaVersion)
  }

  def single(a: Artifact): String =
    singleWithIndent(a, Indent.space(4))

}

object SbtFormatShowPrinter extends ShowPrinter {

  def single(artifact: Artifact): String = {
    val groupId = artifact.groupId
    val version = artifact.maybeVersion.map(v => s""""$v"""").getOrElse("???")

    val groupAndArtifact =
      (artifact.explicitScalaVersion, artifact.maybeScalaVersion) match {
        case (true, Some(scalaVersion)) if artifact.needScalaVersionResolving =>
          s""""$groupId" % "${artifact.artifactIdWithScalaVersion(scalaVersion)}""""
        case (false, Some(_)) =>
          s""""$groupId" %% "${artifact.artifactIdWithoutScalaVersion}""""
        case (_, None) =>
          s""""$groupId" % "${artifact.artifactId}""""
      }

    val prefix = if (artifact.inSequence) "" else "libraryDependencies += "

    artifact.scope match {
      case Some(Test) =>
        s"""$prefix$groupAndArtifact % $version % Test"""
      case _ =>
        s"""$prefix$groupAndArtifact % $version"""
    }
  }
}