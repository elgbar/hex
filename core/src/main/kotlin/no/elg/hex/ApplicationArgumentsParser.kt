package no.elg.hex

import com.xenomachina.argparser.ArgParser

/** @author Elg */
class ApplicationArgumentsParser(parser: ArgParser) {
  val debug by parser.flagging("-d", "--debug", help = "Enable debug overlay and logging")
  val trace by parser.flagging("-t", "--trace", help = "Enable even more logging")
  val silent by parser.flagging(
      "-s", "--silent", help = "Do not print anything to stdout or stderr")
  val mapEditor by parser.flagging(
      "-e", "--map-editor", help = "Start the program in map editor mode")
  val retro by parser.flagging("-r", "--retro", help = "Use only the original textures")
  val cheating by parser.flagging("-c", "--cheating", help = "Have infinite money")
}
