package no.elg.hex

import com.xenomachina.argparser.ArgParser

/**
 * @author Elg
 */
class ApplicationArgumentsParser(parser: ArgParser) {
  val debug by parser.flagging("-d", "--debug", help = "Enable debug overlay")
  val mapEditor by parser.flagging("-e", "--map-editor", help = "Start the program in map editor mode")
}
