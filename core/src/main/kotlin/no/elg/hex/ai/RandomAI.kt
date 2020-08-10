package no.elg.hex.ai

import com.badlogic.gdx.Input.Keys
import kotlin.random.Random.Default as random
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Territory
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

/** @author Elg */
object RandomAI : AI {

  private val buttons =
    arrayOf(Keys.F1, Keys.F2, Keys.F3, Keys.F4, Keys.F5, Keys.F2, Keys.F3)

  @ExperimentalStdlibApi
  override fun action(territory: Territory, gameInputProcessor: GameInputProcessor) {
    val island = territory.island
    island.select(island.hexagons.first())

    do {
      if (island.inHand == null) {

        // hexagons we can pick up pieces from
        val pickUpHexes =
            HashSet<Hexagon<HexagonData>>(
                territory.hexagons.filter {
                  val piece = island.getData(it).piece
                  piece is LivingPiece && !piece.moved
                })

        if (pickUpHexes.isEmpty() || random.nextFloat() < 0.5f) {
          gameInputProcessor.buyUnit(buttons.random())
        } else {
          gameInputProcessor.click(pickUpHexes.random())
        }
      }

      if (island.inHand?.piece is Castle) {

        // hexagons where we can place castles
        val placableHexes =
            HashSet<Hexagon<HexagonData>>(
                territory.hexagons.filter {
                  val piece = island.getData(it).piece
                  !(piece is Castle || piece is Capital || piece is Baron)
                })

        val hexagon = placableHexes.randomOrNull() ?: continue
        gameInputProcessor.click(hexagon)
      } else {

        // hexagon where we can put a living piece
        val attackableHexes =
            HashSet<Hexagon<HexagonData>>(
                territory.hexagons.filter {
                  val piece = island.getData(it).piece
                  !(piece is LivingPiece || piece is Castle || piece is Capital)
                })
        attackableHexes.addAll(territory.enemyBorderHexes)

        val hexagon = attackableHexes.randomOrNull() ?: continue
        gameInputProcessor.click(hexagon)
      }
    } while (random.nextFloat() > 0.005f)
    island.select(null)
  }
}
