package no.elg.hex.island

import com.badlogic.gdx.Gdx
import kotlin.random.Random
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.Team
import no.elg.hex.util.getData
import no.elg.hex.util.noise.FastNoiseLite
import no.elg.hex.util.noise.FractalType.FBM
import no.elg.hex.util.noise.NoiseType.OPEN_SIMPLEX_2_S
import org.hexworks.mixite.core.api.HexagonalGridLayout
import org.hexworks.mixite.core.api.Point

/**
 * @author Elg
 */
object IslandGeneration {

  val noise = FastNoiseLite()

  const val INITIAL_FREQUENCY = 0.1f
  const val INITIAL_FRACTAL_OCTAVES = 3
  const val INITIAL_FRACTAL_LACUNARITY = 2.0f
  const val INITIAL_FRACTAL_GAIN = 0.5f

  /**
   * `a` in a `ax + b` graph
   */
  var amplitude = 0f

  /**
   * `b` in a `ax + b` graph
   */
  var offset = 1f


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
    Gdx.app.debug("ISGEN", "Generating an island $layout of size $width x $height using seed $seed")
    noise.setSeed(seed)
    val island = Island(width, height, layout)
    val random = Random(seed)
    val teams = Team.values()

    for (hexagon in island.hexagons) {
      val data = island.getData(hexagon)
      if (data.edge) continue
      val noise = noiseAt(hexagon.gridX.toFloat(), hexagon.gridZ.toFloat(), width, height)
      Gdx.app.debug("ISGEN", "${hexagon.gridX}, ${hexagon.gridZ} has the noise of $noise")
      if (noise <= 1) {
        data.team = teams.random(random)
        data.setPiece(Empty::class)
      } else {
        data.isOpaque = true
      }
    }

//    island.regenerateCapitals()
    return island
  }
}
