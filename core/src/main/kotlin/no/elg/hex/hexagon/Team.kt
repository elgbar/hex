package no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import no.elg.hex.hexagon.HexType.CUBE
import no.elg.hex.hexagon.HexType.DIAMOND
import no.elg.hex.hexagon.HexType.HALF
import no.elg.hex.hexagon.HexType.HOURGLASS
import no.elg.hex.hexagon.HexType.TRIANGULAR
import no.elg.hex.util.inverse

/** @author Elg */
enum class Team(val color: Color, val type: HexType) {
  LEAF(Color.valueOf("#67CE6E"), HOURGLASS),
  FOREST(Color.valueOf("#36823A"), CUBE),
  STONE(Color.valueOf("#988b8c"), HALF),
  EARTH(Color.valueOf("#BC9953"), TRIANGULAR),
  SUN(Color.valueOf("#c6c81b"), DIAMOND);

  val inverseColor by lazy { color.cpy().inverse() }
}