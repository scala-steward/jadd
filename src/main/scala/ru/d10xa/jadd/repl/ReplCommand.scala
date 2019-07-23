package ru.d10xa.jadd.repl

import com.typesafe.scalalogging.StrictLogging
import org.jline.reader._
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import ru.d10xa.jadd.RunParams

import scala.util.Try

object ReplCommand extends StrictLogging {

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  val nullMaskingCallback: MaskingCallback = null
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  val nullRightPrompt: String = null
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  val nullBuffer: String = null
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  val nullParser: Parser = null

  class ReplContext(
    val prompt: String = "jadd> ",
    val completer: Completer = new JaddJlineCompleter
  ) {

    val builder: TerminalBuilder = TerminalBuilder.builder
    val terminal: Terminal = builder.build
    val reader: LineReader =
      LineReaderBuilder.builder
        .terminal(terminal)
        .completer(completer)
        .parser(nullParser)
        .build

    def readLine(): String =
      reader.readLine(prompt, nullRightPrompt, nullMaskingCallback, nullBuffer)
  }

  def runRepl(runParams: RunParams, action: RunParams => Unit): Unit = {
    logger.info("Welcome to jadd REPL!")
    val replContext = new ReplContext
    var running = true
    while (running) {
      val line: String = readReplString(replContext)
      running = needContinue(line)
      if (running) {
        action(runParams.copy(args = line.split(" ").toVector))
      }
    }
  }

  def needContinue(line: String): Boolean =
    line.trim != "exit" && line.trim != "quit"

  def readReplString(replContext: ReplContext): String =
    Try(replContext.readLine())
      .recover { case _: UserInterruptException => "exit" } // Ctrl + C
      .recover { case _: EndOfFileException => "exit" } // Ctrl + D
      .getOrElse("")

}
