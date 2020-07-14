package no.elg.hex.util

import com.badlogic.gdx.graphics.Color
import kotlin.random.Random

/**
 * @author Elg
 */

/**
 * Inverse the color.
 *
 * @return Same color instance
 */
fun Color.inverse(includeAlpha: Boolean = false): Color {
  return set(1 - r, 1 - g, 1 - b, if (includeAlpha) 1f - a else a)
}

/**
 * Dim the color by the given percent (ie 0.9 -> 10% less bright)
 *
 * @return Same color instance
 */
fun Color.dim(percent: Float): Color {
  return this.apply { mul(r * percent, g * percent, b * percent, 1f) }
}

/**
 * Create a random color
 */
fun randomColor(randomizeAlpha: Boolean = false): Color {
  return Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), if (randomizeAlpha) Random.nextFloat() else 1f)
}
