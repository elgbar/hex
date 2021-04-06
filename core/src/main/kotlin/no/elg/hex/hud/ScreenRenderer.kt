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
import com.badlogic.gdx.utils.Pool.Poolable
import ktx.assets.pool
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

sealed class ScreenText(
  open var color: Color = WHITE,
  open var bold: Boolean = false,
  open var italic: Boolean = false,
  open var next: ScreenText? = null
) {

  abstract val text: String

  val wholeText: String get() = text + (next?.wholeText ?: "")

  val font: BitmapFont
    get() =
      when {
        !Hex.assetsAvailable -> BitmapFont(true)
        !bold && !italic -> Hex.assets.regularFont
        !bold && italic -> Hex.assets.regularItalicFont
        bold && !italic -> Hex.assets.boldFont
        bold && italic -> Hex.assets.boldItalicFont
        else -> error("This should really not happen")
      }

  fun <T : Any> format(
    givenFormat: (ScreenText.(T) -> String)?,
    givenCallable: () -> T
  ): String = givenFormat?.invoke(this, givenCallable()) ?: givenCallable().toString()

  fun <T : Any> formatNullable(
    givenFormat: (ScreenText.(T?) -> String)?,
    givenCallable: () -> T?
  ): String = givenFormat?.invoke(this, givenCallable()) ?: givenCallable().toString()
}

class VariableScreenText<T : Any>(
  private val callable: () -> T,
  color: Color = WHITE,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
  val format: (ScreenText.(T) -> String)? = null,
) : ScreenText(color, bold, italic, next) {

  @Suppress("IMPLICIT_CAST_TO_ANY")
  override val text: String
    get() = format(format, callable)
}

class NullableVariableScreenText<T : Any>(
  val callable: () -> T?,
  color: Color = WHITE,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
  val format: (ScreenText.(T?) -> String)? = null,
) : ScreenText(color, bold, italic, next) {

  @Suppress("IMPLICIT_CAST_TO_ANY")
  override val text: String
    get() = formatNullable(format, callable)
}

@Suppress("SuspiciousVarProperty")
class IfScreenText(val ifupon: () -> ScreenText) : ScreenText() {

  override val text: String get() = ifupon().text

  override var color: Color = WHITE
    get() = ifupon().color

  override var bold: Boolean = false
    get() = ifupon().bold

  override var italic: Boolean = false
    get() = ifupon().italic

  override var next: ScreenText? = null
    get() = ifupon().next
}

val staticTextPool = pool { StaticScreenText("") }

class StaticScreenText(
  override var text: String,
  color: Color = WHITE,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
) : ScreenText(color, bold, italic, next), Poolable {
  override fun reset() {
    text = ""
    color = Color.LIGHT_GRAY
    bold = false
    italic = false
    next = null
  }
}

fun <T : Any> nullCheckedText(
  callable: () -> T?,
  color: Color = WHITE,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
  format: (ScreenText.(T) -> String)? = null
) = NullableVariableScreenText(callable, color, bold, italic, next) { value ->
  if (value != null) {
    this.color = color
    format?.invoke(this, value) ?: value.toString()
  } else {
    this.color = nullText.color
    nullText.text
  }
}

fun <T : Number> signColoredText(
  callable: () -> T,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
  format: (ScreenText.(T) -> String)? = null
): ScreenText {
  return VariableScreenText(callable, bold = bold, italic = italic, next = next) { value ->
    this.color = when (sign(value.toFloat())) {
      1f -> GREEN
      -1f -> RED
      else -> YELLOW
    }
    format?.invoke(this, value) ?: value.toString()
  }
}

/**
 * Display the value if it is outside the given range
 *
 * @param min minimum allowed value of [callable], exclusive
 * @param max maximum allowed value of [callable], exclusive
 */
fun <T : Comparable<T>> validatedText(
  callable: () -> T,
  min: T,
  max: T,
  color: Color = WHITE,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
  format: (ScreenText.(T) -> String)? = null
): ScreenText {
  return VariableScreenText(callable, color, bold, italic, next) { value ->
    this.color = if (value < min || value > max) RED else color
    format?.invoke(this, value) ?: value.toString()
  }
}

fun <T : Comparable<T>> variableText(
  prefix: String,
  callable: () -> T,
  min: T,
  max: T,
  prefixColor: Color = WHITE,
  variableColor: Color = YELLOW,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
  format: (ScreenText.(T) -> String)? = null
): ScreenText {
  return StaticScreenText(
    prefix,
    color = prefixColor,
    bold = bold,
    italic = italic,
    next =
    validatedText(
      callable,
      min,
      max,
      bold = bold,
      italic = italic,
      color = variableColor,
      format = format,
      next = next
    )
  )
}

fun <T : Any> prefixText(
  prefix: String,
  callable: () -> T,
  prefixColor: Color = WHITE,
  variableColor: Color = YELLOW,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null,
  format: (ScreenText.(T) -> String)? = null
): ScreenText {
  return staticTextPool.obtain().also {
    it.text = prefix
    it.color = prefixColor
    it.bold = bold
    it.italic = italic
    it.next = VariableScreenText(
      callable = callable,
      bold = bold,
      italic = italic,
      color = variableColor,
      format = format,
      next = next
    )
  }
}

private val nullText = StaticScreenText("null", color = RED)

fun nullText() = nullText
fun nullText(next: ScreenText) = StaticScreenText("null", color = RED, next = next)

private val emptyText = StaticScreenText("")
fun emptyText(): ScreenText = staticTextPool.obtain()

fun booleanText(
  callable: () -> Boolean,
  bold: Boolean = false,
  italic: Boolean = false,
  next: ScreenText? = null
) =
  VariableScreenText(
    callable,
    bold = bold,
    italic = italic,
    next = next
  ) { bool ->
    color = if (bool) GREEN else RED
    if (bool) "true " else "false"
  }

object ScreenRenderer : Disposable, Resizable {

  private val spacing: Float by lazy { if (Hex.assetsAvailable) Hex.assets.fontSize / 2f else 20f }

  @Suppress("LibGDXStaticResource")
  private lateinit var batch: SpriteBatch
  internal val camera = OrthographicCamera().also { it.setToOrtho(true) }

  var draws = 0
    private set

  fun ScreenText.draw(
    line: Int,
    position: ScreenDrawPosition = TOP_LEFT,
    offsetX: Float = spacing,
    lines: Int = 1,
  ) {
    val text = text
    if (text.isNotEmpty()) {
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
    } else {
      next?.draw(line, position, offsetX, lines)
    }
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
    draws++
    batch.begin()
  }

  fun resetDraws() {
    draws = 0
  }

  fun end() {
    batch.end()
  }

  override fun dispose() {
    batch.dispose()
  }

  fun resume() {
    batch = SpriteBatch()
  }

  override fun resize(width: Int, height: Int) {
    camera.setToOrtho(true)
    batch.projectionMatrix = camera.combined
  }
}
