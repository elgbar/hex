package no.elg.hex.island

import com.badlogic.gdx.Gdx
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.Team
import no.elg.hex.hexagon.replaceWithTree
import no.elg.hex.util.getData
import no.elg.hex.util.info
import no.elg.hex.util.noise.FastNoiseLite
import no.elg.hex.util.noise.FractalType.FBM
import no.elg.hex.util.noise.NoiseType.OPEN_SIMPLEX_2_S
import no.elg.hex.util.reportTiming
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.HexagonalGridLayout
import org.hexworks.mixite.core.api.Point
import kotlin.random.Random

/**
 * @author Elg
 */
object IslandGeneration {

  val noise = FastNoiseLite()

  const val INITIAL_FREQUENCY = 0.14f
  const val INITIAL_FRACTAL_OCTAVES = 1
  const val INITIAL_FRACTAL_LACUNARITY = 2.1f
  const val INITIAL_FRACTAL_GAIN = 0.7f

  /**
   * `a` in a `ax + b` graph
   */
  var amplitude = 0.07f

  /**
   * `b` in a `ax + b` graph
   */
  var offset = 0.4f

  init {
    noise.setNoiseType(OPEN_SIMPLEX_2_S)
    noise.setFractalType(FBM)

    noise.setFrequency(INITIAL_FREQUENCY)
    noise.setFractalOctaves(INITIAL_FRACTAL_OCTAVES)
    noise.setFractalLacunarity(INITIAL_FRACTAL_LACUNARITY)
    noise.setFractalGain(INITIAL_FRACTAL_GAIN)
  }

  fun noiseAt(x: Float, y: Float, width: Int, height: Int): Double {
    val origin = Point.fromPosition(width / 2.0, height / 2.0)
    val hexPoint = Point.fromPosition(x.toDouble(), y.toDouble())

    return origin.distanceFrom(hexPoint) * amplitude + noise.getNoise(x, y) + offset
  }

  fun generate(seed: Int, width: Int, height: Int, layout: HexagonalGridLayout): Island {
    Gdx.app.info("ISGEN"){"Generating an island $layout of size $width x $height using seed $seed"}
    val island: Island
    reportTiming("generate island") {
      noise.setSeed(seed)
      island = Island(width, height, layout)
      val random = Random(seed)
      val teams = Team.values()

    for (hexagon in island.hexagons) {
        val data = island.getData(hexagon)
        data.setPiece<Empty>()
        if (data.edge) continue
        val noise = noiseAt(hexagon.gridX.toFloat(), hexagon.gridZ.toFloat(), width, height)
        Gdx.app.trace("ISGEN", "${hexagon.gridX}, ${hexagon.gridZ} has the noise of $noise")
        if (noise <= 1) {
        data.team = teams.random(random)
      } else {
        data.isOpaque = true
      }
    }
    return island

//    island.regenerateCapitals()
    return island
  }
}