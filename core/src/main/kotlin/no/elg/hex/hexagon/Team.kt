package no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import no.elg.hex.hexagon.HexType.CUBE
import no.elg.hex.hexagon.HexType.DIAMOND
import no.elg.hex.hexagon.HexType.HALF
import no.elg.hex.hexagon.HexType.HOURGLASS
import no.elg.hex.hexagon.HexType.TRIANGULAR

/** @author Elg */
enum class Team(val color: Color, val type: HexType) {
  SUN(Color.valueOf("#FFE049"), DIAMOND),
  LEAF(Color.valueOf("#67CE6E"), HOURGLASS),
  FOREST(Color.valueOf("#36823A"), CUBE),
  EARTH(Color.valueOf("#BC9953"), TRIANGULAR),
  STONE(Color.valueOf("#7F7172"), HALF)
}