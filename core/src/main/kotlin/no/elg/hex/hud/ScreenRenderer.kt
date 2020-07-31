package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.GREEN
import com.badlogic.gdx.graphics.Color.RED
import com.badlogic.gdx.graphics.Color.WHITE
import com.badlogic.gdx.graphics.Color.YELLOW
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Assets.Companion.FONT_SIZE
import no.elg.hex.Hex
import no.elg.hex.api.Resizable
import no.elg.hex.hud.ScreenDrawPosition.TOP
import kotlin.math.sign


enum class ScreenDrawPosition(val bottom: Boolean, val right: Boolean) {
  TOP(false, false),
  BOTTOM(true, false),
  TOP_RIGHT(false, true),
  BOTTOM_RIGHT(true, true)
}

data class ScreenText(
  val text: String,
  val color: Color = WHITE,
  val bold: Boolean = false,
  val italic: Boolean = false,
  val next: ScreenText? = null
) {
  val font: BitmapFont = when {
    !bold && !italic -> Hex.assets.regularFont
    !bold && italic -> Hex.assets.regularItalicFont
    bold && !italic -> Hex.assets.boldFont
    bold && italic -> Hex.assets.boldItalicFont
    else -> error("This should really not happen!")
  }
}

fun <T> nullCheckedText(
  value: T?,
  color: Color = WHITE,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
  format: (T) -> String = { it.toString() }
) = if (value == null) nullText(next) else ScreenText(format(value), color, bold, italic, next)

fun <T : Number> signColoredText(value: T,
                                 bold: Boolean = false,
                                 italic: Boolean = false,
                                 next: ScreenText? = null,
                                 format: (T) -> String = { it.toString() }): ScreenText {
  val signColor = when (sign(value.toFloat())) {
    1f -> GREEN
    -1f -> RED
    else -> YELLOW
  }

  return ScreenText(format(value), signColor, bold, italic, next)
}

/**
 * Display the value if it is outside the given range
 *
 * @param min minimum allowed value of [value], exclusive
 * @param max maximum allowed value of [value], exclusive
 */
fun <T : Comparable<T>> validatedText(
  value: T,
  min: T,
  max: T,
  color: Color = WHITE,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
  format: (T) -> String = { it.toString() }
): ScreenText {
  return if (value < min || value > max) {
    ScreenText(format(value), color = RED, bold = true, next = next)
  } else {
    ScreenText(format(value), color, bold, italic, next)
  }
}

fun <T : Comparable<T>> variableText(
  prefix: String,
  value: T,
  min: T,
  max: T,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
  format: (T) -> String = { it.toString() }
): ScreenText {
  return ScreenText(prefix, color = WHITE, bold = bold, italic = italic,
    next = validatedText(value, min, max, bold = bold, italic = italic, color = YELLOW, format = format, next = next))
}

fun nullText(next: ScreenText? = null) = ScreenText(
  "null",
  RED,
  bold = true,
  italic = false,
  next = next
)

fun emptyText() = ScreenText("")

fun booleanText(
  check: Boolean,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null
) = ScreenText(
  "%-5s".format(check),
  bold = bold,
  italic = italic,
  color = if (check) GREEN else RED,
  next = next
)

object ScreenRenderer : Disposable, Resizable {

  val spacing: Float = FONT_SIZE * Hex.scale / 2f
  val batch: SpriteBatch = SpriteBatch()

  init {
    resize(Gdx.graphics.width, Gdx.graphics.height)
  }

  fun ScreenText.draw(line: Int, position: ScreenDrawPosition = TOP, offsetX: Float = spacing) {
    val y = if (position.bottom) {
      spacing * line * 2f + spacing
    } else {
      Gdx.graphics.height - spacing * line * 2f
    }

    val (x, nextOffsetX) = if (position.right) {
      var ctr = 1f
      var curr: ScreenText? = next
      while (curr != null) {
        ctr += curr.text.length
        curr = curr.next
      }
      val totalLength = Gdx.graphics.width - spacing * ctr

      totalLength - spacing * text.length to totalLength
    } else offsetX to offsetX + spacing * text.length

    font.color = color
    font.draw(batch, text, x, y)
    next?.draw(line, position, nextOffsetX)
  }

  /**
   * Draw all given text on different lines
   */
  fun drawAll(vararg screenTexts: ScreenText, position: ScreenDrawPosition = TOP) {

    if (position.bottom) {
      screenTexts.reverse()
    }

    begin()
    for ((line, screenText) in screenTexts.withIndex()) {
      screenText.draw(line + 1, position)
    }
    end()
  }

  fun begin() {
    batch.begin()
  }

  fun end() {
    batch.end()
  }

  override fun dispose() {
    batch.dispose()
  }

  override fun resize(width: Int, height: Int) {
    batch.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
  }

}
