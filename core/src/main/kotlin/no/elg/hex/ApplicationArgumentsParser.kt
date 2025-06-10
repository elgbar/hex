@file:Suppress("PropertyName")

package no.elg.hex

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

/** @author Elg */
class ApplicationArgumentsParser(parser: ArgParser) {
  val debug by parser.flagging("-d", "--debug", help = "Enable debug logging")
  val trace by parser.flagging("-t", "--trace", help = "Enable even more logging")
  val silent by parser.flagging(
    "-s",
    "--silent",
    help = "Do not print anything to stdout or stderr"
  )
  val mapEditor by parser.flagging(
    "-e",
    "--map-editor",
    help = "Start the program in map editor mode"
  )
  val cheating by parser.flagging("--i-am-a-cheater", help = "Enable cheating")
  val `disable-island-loading` by parser.flagging("Don't load islands")
  val `draw-edges` by parser.flagging("Draw the edge hexagons to assists with debugging")
  val `stage-debug` by parser.flagging("Enable debug overlay for UI")
  val `update-previews` by parser.flagging("Update pre-rendered previews of islands from the initial island metadata")
  val `update-saved-islands` by parser.flagging("Update the saved islands by saving them again after loading them. Only really useful when changing the save format")
  val `load-all-islands` by parser.flagging("Load all islands at startup instead of when first played")
  val `reset-all` by parser.flagging("Resetting settings and progress")
  val `ai-debug` by parser.flagging("Listen to the AI thinking")
  val `create-artb-improvement-rapport` by parser.flagging("List all islands where the Author Round to Beat (ARtB) was beaten")
  val `validate-island-on-load` by parser.flagging("Validate island when loading it, useful with `--load-all-islands`")

  val scale by parser.storing("Scale of UI, if <= 0 default scale apply") { toInt() }.default { 0 }
  val profile by parser.flagging("Enable GL profiling")
}