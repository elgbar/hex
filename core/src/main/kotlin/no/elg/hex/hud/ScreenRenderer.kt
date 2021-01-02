package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.GREEN
import com.badlogic.gdx.graphics.Color.RED
import com.badlogic.gdx.graphics.Color.WHITE
import com.badlogic.gdx.graphics.Color.YELLOW
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex
import no.elg.hex.api.Resizable
import no.elg.hex.hud.ScreenDrawPosition.HorizontalPosition.HORIZONTAL_CENTER
import no.elg.hex.hud.ScreenDrawPosition.HorizontalPosition.LEFT
import no.elg.hex.hud.ScreenDrawPosition.HorizontalPosition.RIGHT
import no.elg.hex.hud.ScreenDrawPosition.TOP_LEFT
import no.elg.hex.hud.ScreenDrawPosition.VerticalPosition.BOTTOM
import no.elg.hex.hud.ScreenDrawPosition.VerticalPosition.TOP
import no.elg.hex.hud.ScreenDrawPosition.VerticalPosition.VERTICAL_CENTER
import kotlin.math.sign

enum class ScreenDrawPosition(val vertical: VerticalPosition, val horizontal: HorizontalPosition) {
  TOP_LEFT(TOP, LEFT),
  TOP_CENTER(TOP, HORIZONTAL_CENTER),
  TOP_RIGHT(TOP, RIGHT),
  CENTER_LEFT(VERTICAL_CENTER, LEFT),
  CENTER_CENTER(VERTICAL_CENTER, HORIZONTAL_CENTER),
  CENTER_RIGHT(VERTICAL_CENTER, RIGHT),
  BOTTOM_LEFT(BOTTOM, LEFT),
  BOTTOM_CENTER(BOTTOM, HORIZONTAL_CENTER),
  BOTTOM_RIGHT(BOTTOM, RIGHT);

  enum class HorizontalPosition {
    LEFT,
    HORIZONTAL_CENTER,
    RIGHT
  }

  enum class VerticalPosition {
    TOP,
    VERTICAL_CENTER,
    BOTTOM
  }
}

data class ScreenText(
  val any: Any,
  val color: Color = WHITE,
  val bold: Boolean = false,
  val italic: Boolean = false,
  val next: ScreenText? = null
) {

  val text: String = any.toString()
  val wholeText: String = text + (next?.wholeText ?: "")

  val font: BitmapFont by lazy {
    when {
      !Hex.assetsAvailable -> BitmapFont(true)
      !bold && !italic -> Hex.assets.regularFont
      !bold && italic -> Hex.assets.regularItalicFont
      bold && !italic -> Hex.assets.boldFont
      bold && italic -> Hex.assets.boldItalicFont
      else -> error("This should really not happen")
    }
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

fun <T : Number> signColoredText(
  value: T,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
  format: (T) -> String = { it.toString() }
): ScreenText {
  val signColor =
    when (sign(value.toFloat())) {
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
  return ScreenText(
    prefix,
    color = WHITE,
    bold = bold,
    italic = italic,
    next =
    validatedText(
      value,
      min,
      max,
      bold = bold,
      italic = italic,
      color = YELLOW,
      format = format,
      next = next
    )
  )
}

fun nullText(next: ScreenText? = null) =
  ScreenText("null", RED, bold = true, italic = false, next = next)

private val emptyText = ScreenText("")
fun emptyText() = emptyText

fun booleanText(
  check: Boolean,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null
) =
  ScreenText(
    "%-5s".format(check),
    bold = bold,
    italic = italic,
    color = if (check) GREEN else RED,
    next = next
  )

object ScreenRenderer : Disposable, Resizable {

  val spacing: Float by lazy { if (Hex.assetsAvailable) Hex.assets.fontSize / 2f else 20f }
  private val batch: SpriteBatch = SpriteBatch()
  internal val camera = OrthographicCamera()

  init {
    resize(Gdx.graphics.width, Gdx.graphics.height)
  }

  fun ScreenText.draw(
    line: Int,
    position: ScreenDrawPosition = TOP_LEFT,
    offsetX: Float = spacing,
    lines: Int = 1,
  ) {
    val y =
      when (position.vertical) {
        TOP -> spacing * line * 2f + spacing
        BOTTOM -> Gdx.graphics.height - spacing * line * 2f
        VERTICAL_CENTER -> Gdx.graphics.height / 2 - spacing * (line - lines / 2) * 2f
      }

    fun totalLength(): Float {
      var ctr = 1f
      var curr: ScreenText? = next
      while (curr != null) {
        ctr += curr.text.length
        curr = curr.next
      }
      return Gdx.graphics.width - spacing * ctr
    }

    val (x, nextOffsetX) = when (position.horizontal) {
      RIGHT -> {
        val totalLength = totalLength()
        totalLength - spacing * text.length to totalLength
      }
      LEFT -> offsetX to offsetX + spacing * text.length
      HORIZONTAL_CENTER -> {
        require(next == null) { "Horizontal centred text cannot have a next element" }
        (Gdx.graphics.width - wholeText.length * spacing) / 2f to 0f
      }
    }
    font.color = color
    font.draw(batch, text, x, y)
    next?.draw(line, position, nextOffsetX, lines)
  }

  /** Draw all given text on different lines */
  fun drawAll(vararg screenTexts: ScreenText, position: ScreenDrawPosition = TOP_LEFT) {

    if (position.vertical !== TOP) {
      screenTexts.reverse()
    }
    val offset = if (position.vertical == TOP) 0 else 1

    begin()
    for ((line, screenText) in screenTexts.withIndex()) {
      screenText.draw(line + offset, position, lines = screenTexts.size)
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
    camera.setToOrtho(true)
    batch.projectionMatrix = camera.combined
  }
}
