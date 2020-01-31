package ru.d10xa.jadd.run

import better.files._
import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.Sync
import cats.implicits._
import ru.d10xa.jadd.cli.Command.Help
import ru.d10xa.jadd.cli.Command.Repl
import ru.d10xa.jadd.cli.Config
import ru.d10xa.jadd.core.Ctx
import ru.d10xa.jadd.core.Loader
import ru.d10xa.jadd.core.ProjectFileReaderImpl
import ru.d10xa.jadd.core.Utils
import ru.d10xa.jadd.pipelines._
import ru.d10xa.jadd.shortcuts.ArtifactInfoFinder
import ru.d10xa.jadd.shortcuts.ArtifactShortcuts
import ru.d10xa.jadd.shortcuts.RepositoryShortcutsImpl

trait CommandExecutor {
  def execute(config: Config, loader: Loader, showUsage: () => Unit): Unit
}

class CommandExecutorImpl extends CommandExecutor {

  override def execute(
    config: Config,
    loader: Loader,
    showUsage: () => Unit): Unit =
    config match {
      case c if c.command == Repl =>
        () // already in repl
      case c if c.command == Help =>
        showUsage()
      case c =>
        val repositoryShortcuts = RepositoryShortcutsImpl
        val artifactInfoFinder: ArtifactInfoFinder =
          new ArtifactInfoFinder(
            artifactShortcuts = new ArtifactShortcuts(
              Utils.sourceFromSpringUri(config.shortcutsUri)),
            repositoryShortcuts = repositoryShortcuts
          )
        CommandExecutorImpl
          .executePipelines[IO](c, loader, artifactInfoFinder)
          .unsafeRunSync()
    }

}

object CommandExecutorImpl {

  def filterPipelines[F[_]: Sync](
    pipelines: List[Pipeline]
  ): F[List[Pipeline]] =
    pipelines
    // TODO recover is not safe
      .map(p => p.applicable().recover { case _ => false }.map(_ -> p))
      .sequence
      .map(_.collect { case (b, p) if b => p })

  def executePipelines[F[_]: Sync](
    config: Config,
    loader: Loader,
    artifactInfoFinder: ArtifactInfoFinder
  ): F[Unit] = {

    val ctx = Ctx(config)

    val projectFileReaderImpl = new ProjectFileReaderImpl(
      File(config.projectDir))
    val pipelines: List[Pipeline] = List(
      new GradlePipeline(ctx, artifactInfoFinder),
      new MavenPipeline(ctx, artifactInfoFinder),
      new SbtPipeline(
        ctx,
        artifactInfoFinder,
        projectFileReaderImpl
      ),
      new AmmonitePipeline(ctx, projectFileReaderImpl)
    )

    def orDefaultPipeline(pipelines: List[Pipeline]): NonEmptyList[Pipeline] =
      NonEmptyList
        .fromList(pipelines)
        .getOrElse(
          NonEmptyList.of(new UnknownProjectPipeline(ctx, artifactInfoFinder)))

    def runPipelines(pipelines: NonEmptyList[Pipeline]): F[Unit] =
      pipelines.map(_.run(loader)).sequence_

    def activePipelines(): F[NonEmptyList[Pipeline]] =
      filterPipelines(pipelines)
        .map(orDefaultPipeline)

    def runActivePipelines(): F[Unit] =
      activePipelines()
        .flatMap(runPipelines)

    runActivePipelines()

  }
}