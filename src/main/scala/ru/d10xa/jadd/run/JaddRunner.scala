package ru.d10xa.jadd.run

import java.net.URI

import com.typesafe.scalalogging.StrictLogging
import ru.d10xa.jadd.cli.Command.Repl
import ru.d10xa.jadd.cli.Cli
import ru.d10xa.jadd.cli.Config
import ru.d10xa.jadd.core.Loader.LoaderImpl
import ru.d10xa.jadd.repl.ReplCommand
import ru.d10xa.jadd.shortcuts.ArtifactInfoFinder
import ru.d10xa.jadd.shortcuts.ArtifactShortcuts
import ru.d10xa.jadd.shortcuts.RepositoryShortcutsImpl
import ru.d10xa.jadd.core.ProxySettings
import ru.d10xa.jadd.core.Utils
import ru.d10xa.jadd.log.LoggingUtil

class JaddRunner(
  cli: Cli,
  loggingUtil: LoggingUtil
) {

  def readAndEvalConfig(args: Vector[String]): Config = {
    val config = cli.parse(args)
    if (config.debug) loggingUtil.enableDebug()
    if (config.quiet) loggingUtil.quiet()
    config.proxy.foreach(initProxy)
    config
  }

  def initProxy(proxyStr: String): Unit = {
    val proxyUri = new URI(proxyStr)
    val proxySettings = ProxySettings.fromURI(proxyUri)
    ProxySettings.set(proxySettings)
    (proxySettings.httpProxyUser, proxySettings.httpProxyPassword) match {
      case (Some(u), Some(p)) => ProxySettings.setupAuthenticator(u, p)
      case _ => // do nothing
    }
  }

  def run(runParams: RunParams): Unit = {

    def runOnceForRepl(runParams: RunParams): Unit =
      JaddRunner.runOnce(
        readAndEvalConfig(runParams.args),
        runParams.commandExecutor)

    val config: Config = readAndEvalConfig(runParams.args)
    if (config.command == Repl) ReplCommand.runRepl(runParams, runOnceForRepl)
    else JaddRunner.runOnce(config, runParams.commandExecutor)
  }

}

object JaddRunner extends StrictLogging {
  def runOnce(config: Config, commandExecutor: CommandExecutor): Unit = {
    val repositoryShortcuts = RepositoryShortcutsImpl
    val artifactInfoFinder: ArtifactInfoFinder =
      new ArtifactInfoFinder(
        artifactShortcuts =
          new ArtifactShortcuts(Utils.sourceFromSpringUri(config.shortcutsUri)),
        repositoryShortcuts = repositoryShortcuts
      )
    val loader = new LoaderImpl(artifactInfoFinder, repositoryShortcuts)
    commandExecutor.execute(config, loader, () => logger.info(config.usage))
  }
}