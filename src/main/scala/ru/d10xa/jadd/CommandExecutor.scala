package ru.d10xa.jadd

import ru.d10xa.jadd.cli.Command.Analyze
import ru.d10xa.jadd.cli.Command.Help
import ru.d10xa.jadd.cli.Command.Repl
import ru.d10xa.jadd.cli.Config
import ru.d10xa.jadd.pipelines.GradlePipeline
import ru.d10xa.jadd.pipelines.MavenPipeline
import ru.d10xa.jadd.pipelines.Pipeline
import ru.d10xa.jadd.pipelines.SbtPipeline
import ru.d10xa.jadd.pipelines.UnknownProjectPipeline
import ru.d10xa.jadd.shortcuts.ArtifactInfoFinder
import ru.d10xa.jadd.shortcuts.ArtifactShortcuts
import ru.d10xa.jadd.shortcuts.RepositoryShortcutsImpl

trait CommandExecutor {
  def execute(config: Config, showUsage: () => Unit): Unit
}

class CommandExecutorImpl extends CommandExecutor {

  override def execute(config: Config, showUsage: () => Unit): Unit = {
    config match {
      case c if c.command == Repl =>
        Unit // already in repl
      case c if c.command == Analyze =>
        analyze.run(Ctx(c))
      case c if c.command == Help =>
        showUsage()
      case c =>
        executePipelines(c)
    }
  }

  def executePipelines(config: Config): Unit = {
    implicit val artifactInfoFinder: ArtifactInfoFinder =
      new ArtifactInfoFinder(
        artifactShortcuts = new ArtifactShortcuts(Utils.sourceFromSpringUri(config.shortcutsUri)),
        repositoryShortcuts = RepositoryShortcutsImpl
      )
    val ctx = Ctx(config)
    val pipelines: List[Pipeline] = List(
      new GradlePipeline(ctx),
      new MavenPipeline(ctx),
      new SbtPipeline(ctx)
    )

    val activePipelines =
      Option(pipelines.filter(_.applicable))
        .filter(_.nonEmpty)
        .getOrElse(Seq(new UnknownProjectPipeline(ctx)))

    activePipelines.foreach(_.run())
  }

}