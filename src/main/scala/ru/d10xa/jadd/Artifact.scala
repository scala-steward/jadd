package ru.d10xa.jadd

import ru.d10xa.jadd.troubles.ArtifactTrouble
import ru.d10xa.jadd.troubles.WrongArtifactRaw

final case class Artifact(
  groupId: String,
  artifactId: String,
  maybeVersion: Option[String] = None,
  shortcut: Option[String] = None,
  scope: Option[Scope] = None,
  repository: Option[String] = None,
  metadataUrl: Option[String] = None,
  maybeScalaVersion: Option[String] = None,
  availableVersions: Seq[String] = Seq.empty
) {

  def needScalaVersionResolving: Boolean = artifactId.contains("%%")

  def asPath: String = {
    val groupIdPath = groupId.replace('.', '/')
    val art =
      if (needScalaVersionResolving && maybeScalaVersion.isDefined) artifactIdWithScalaVersion(maybeScalaVersion.get)
      else artifactId
    val l: Seq[String] = groupIdPath :: art :: Nil
    maybeVersion
      .map(l :+ _)
      .getOrElse(l)
      .mkString("/")
  }

  def artifactIdWithScalaVersion(v: String): String = {
    require(artifactId.contains("%%"), "scala version resolving require placeholder %%")
    artifactId.replace("%%", s"_$v")
  }

}

object Artifact {

  def fromString(artifactRaw: String): Either[ArtifactTrouble, Artifact] = {
    import cats.syntax.either._
    artifactRaw.split(":") match {
      case Array(a, b) =>
        Artifact(
          groupId = a,
          artifactId = b
        ).asRight
      case _ => WrongArtifactRaw.asLeft
    }
  }
}
