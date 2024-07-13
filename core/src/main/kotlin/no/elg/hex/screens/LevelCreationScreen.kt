package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.BLACK
import com.badlogic.gdx.graphics.Color.WHITE
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Pixmap.Blending.None
import com.badlogic.gdx.graphics.Pixmap.Filter.NearestNeighbour
import com.badlogic.gdx.graphics.Pixmap.Format.RGB565
import com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import com.kotcrab.vis.ui.util.form.FormInputValidator
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.spinner.ArraySpinnerModel
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.SimpleFloatSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.Spinner
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.assets.disposeSafely
import ktx.graphics.use
import ktx.scene2d.actor
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visImage
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.island.IslandGeneration
import no.elg.hex.island.IslandGeneration.INITIAL_FRACTAL_GAIN
import no.elg.hex.island.IslandGeneration.INITIAL_FRACTAL_LACUNARITY
import no.elg.hex.island.IslandGeneration.INITIAL_FRACTAL_OCTAVES
import no.elg.hex.island.IslandGeneration.INITIAL_FREQUENCY
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.util.onInteract
import no.elg.hex.util.play
import no.elg.hex.util.value
import org.hexworks.mixite.core.api.HexagonalGridLayout
import org.hexworks.mixite.core.api.HexagonalGridLayout.HEXAGONAL
import org.hexworks.mixite.core.api.HexagonalGridLayout.RECTANGULAR
import org.hexworks.mixite.core.api.HexagonalGridLayout.TRAPEZOID
import org.hexworks.mixite.core.api.HexagonalGridLayout.TRIANGULAR
import kotlin.random.Random
import com.badlogic.gdx.utils.Array as GdxArray

/** @author Elg */
class LevelCreationScreen : StageScreen(), ReloadableScreen {

  private val disableables = mutableListOf<Disableable>()
  private val previewSize get() = (((Gdx.graphics.width - 3 * (Gdx.graphics.width * 0.025f)) / 2).toInt() * 2).coerceAtLeast(1024)

  private val layoutSpinner = ArraySpinnerModel(GdxArray(HexagonalGridLayout.entries.toTypedArray()))
  private val widthSpinner = IntSpinnerModel(31, 1, Int.MAX_VALUE)
  private val heightSpinner = IntSpinnerModel(31, 1, Int.MAX_VALUE)
  private val seedField = VisTextField(Random.nextLong().toString())

  private val previewImage: VisImage
  private var previewImageNoise: VisImage? = null
  private var previewBuffer: FrameBuffer? = null
  private val previewBufferNoise: FrameBuffer? = null
  private var pixmap: Pixmap? = null
  private val dummyMetadata = FastIslandMetadata(-1)

  private val validator = object : FormInputValidator("Invalid width/height for given layout") {
    override fun validate(input: String?): Boolean {
      val valid =
        layoutSpinner.value?.gridLayoutStrategy?.checkParameters(widthSpinner.value, heightSpinner.value)
          ?: true

      for (disableable in disableables) {
        disableable.isDisabled = !valid
      }

      return valid
    }
  }

  init {
    rootTable {
      // arbitrary initial value

      visTable { table ->
        previewImage = visImage(Texture(previewSize, previewSize, RGBA8888)) {
          this.setScaling(Scaling.fit)
          it.fillX()
        }

        if (Hex.debugStage) {
          previewImageNoise = visImage(Texture(NOISE_SIZE, NOISE_SIZE, RGB565)) {
            this.setScaling(Scaling.fillY)
            it.right()
            it.width(0f)
          }
        }
      }

      row()

      var oldWidth = widthSpinner.value
      var oldHeight = heightSpinner.value

      fun syncValue(changed: IntSpinnerModel, other: IntSpinnerModel, oldValue: Int) {
        val currentValue = changed.value
        if (layoutSpinner.value == TRIANGULAR) {
          other.setValue(currentValue, false)
        } else if (layoutSpinner.value == HEXAGONAL) {
          val delta = (currentValue - oldValue).coerceIn(-1, 1)
          val newValue = currentValue + (1 - (currentValue % 2)) * delta
          changed.setValue(newValue, false)
          other.setValue(newValue, false)
        }

        oldWidth = widthSpinner.value
        oldHeight = heightSpinner.value
      }

      val spinner: Spinner
      horizontalGroup {
        space(20f)

        spinner("Width", widthSpinner) {
          name = WIDTH_SPINNER_NAME
          textField.addValidator(validator)
          onChange {
            syncValue(widthSpinner, heightSpinner, oldWidth)
            renderPreview()
          }
        }
        spinner("Height", heightSpinner) {
          textField.addValidator(validator)
          onChange {
            syncValue(heightSpinner, widthSpinner, oldHeight)
            renderPreview()
          }
        }
        spinner = spinner("Layout", layoutSpinner) {
          val minWidth =
            Hex.assets.fontSize *
              HexagonalGridLayout.entries.toTypedArray().maxOf { layout -> layout.name.length / 2f + 1 }
          cells.get(1)?.minWidth(minWidth)
          textField.addValidator(validator)
          onChange { renderPreview() }
        }
      }

      row()
      horizontalGroup {
        space(20f)
        spinner(
          "Frequency",
          SimpleFloatSpinnerModel(INITIAL_FREQUENCY, 0f, 10f, 0.01f, 3).also {
            onChange {
              IslandGeneration.noise.setFrequency(it.value)
              renderPreview()
            }
          }
        )
        spinner(
          "Amplitude",
          SimpleFloatSpinnerModel(IslandGeneration.amplitude, -1000f, 1000f, .01f, 3).also {
            onChange {
              IslandGeneration.amplitude = it.value
              renderPreview()
            }
          }
        )
        spinner(
          "Offset",
          SimpleFloatSpinnerModel(IslandGeneration.offset, -1000f, 1000f, .1f, 2).also {
            onChange {
              IslandGeneration.offset = it.value
              renderPreview()
            }
          }
        )
        spinner(
          "Tree chance",
          SimpleFloatSpinnerModel(IslandGeneration.treeChance, 0f, 1f, .01f, 3).also {
            onChange {
              IslandGeneration.treeChance = it.value
              renderPreview()
            }
          }
        )
      }
      row()
      horizontalGroup {
        space(20f)
        spinner(
          "Fractal Octaves",
          IntSpinnerModel(INITIAL_FRACTAL_OCTAVES, 1, 9, 1).also {
            onChange {
              IslandGeneration.noise.setFractalOctaves(it.value)
              renderPreview()
            }
          }
        )
        spinner(
          "Fractal Lacunarity",
          SimpleFloatSpinnerModel(INITIAL_FRACTAL_LACUNARITY, 0f, 10f, 0.1f).also {
            onChange {
              IslandGeneration.noise.setFractalLacunarity(it.value)
              renderPreview()
            }
          }
        )
        spinner(
          "Fractal Gain",
          SimpleFloatSpinnerModel(INITIAL_FRACTAL_GAIN, 0f, 1f, 0.05f).also {
            onChange {
              IslandGeneration.noise.setFractalGain(it.value)
              renderPreview()
            }
          }
        )
      }
      row()
      horizontalGroup {
        space(10f)
        visLabel("Island seed")
        addActor(
          actor(seedField) {
            onChange {
              renderPreview()
            }
          }
        )
        visTextButton("Randomize") {
          onClick {
            seedField.text = Random.nextLong().toString()
            renderPreview()
          }
        }
      }
      row()

      fun layoutExplanation(): String =
        when (layoutSpinner.value) {
          RECTANGULAR -> "A rectangular layout has no special rules."
          HEXAGONAL -> "The hexagonal layout must have equal width and height and it must be odd."
          TRAPEZOID -> "A trapezoid layout has no special rules."
          TRIANGULAR -> "A triangular layout must have equal width and height."
          else -> "Invalid layout: ${layoutSpinner.value}"
        }
      visLabel(layoutExplanation()) { spinner.onChange { setText(layoutExplanation()) } }

      row()

      horizontalGroup {
        space(10f)
        visTextButton("Create island") {
          disableables.add(this)
          onClick {
            if (this.isDisabled) return@onClick
            val nextId = Hex.assets.islandFiles.nextIslandId
            Gdx.app.debug(
              "CREATOR",
              "Creating island $nextId with a dimension of " + "${widthSpinner.value} x ${heightSpinner.value} and layout ${layoutSpinner.value}"
            )
            play(FastIslandMetadata(nextId), createIsland())
          }
        }

        visTextButton("Cancel") {
          onInteract(stage, keyShortcut = intArrayOf(Keys.ESCAPE)) {
            Hex.screen = LevelSelectScreen()
          }
        }
      }

      pack()
    }
  }

  override fun recreate(): AbstractScreen {
    return LevelCreationScreen().also {
      it.layoutSpinner.value = layoutSpinner.value
      it.widthSpinner.value = widthSpinner.value
      it.heightSpinner.value = heightSpinner.value
      it.seedField.text = seedField.text
      validator.validateInput(null)
      renderPreview()
    }
  }

  override fun render(delta: Float) {
    super.render(delta)

    if (Hex.debugStage) {
      lineRenderer.begin(ShapeType.Line)
      lineRenderer.color = Color.LIGHT_GRAY
      lineRenderer.line(Gdx.graphics.width / 2f, 0f, Gdx.graphics.width / 2f, Gdx.graphics.height.toFloat())
      lineRenderer.line(0f, Gdx.graphics.height / 2f, Gdx.graphics.width.toFloat(), Gdx.graphics.height / 2f)
      lineRenderer.end()
    }
  }

  private fun createIsland(): Island {
    return IslandGeneration.generate(
      seedField.text.hashCode(),
      widthSpinner.value + 2,
      heightSpinner.value + 2,
      layoutSpinner.value
    )
  }

  private var lastRequestedReRender = 0L

  private fun renderPreview() {
    if (Gdx.graphics.frameId == lastRequestedReRender) {
      return
    }
    lastRequestedReRender = Gdx.graphics.frameId
    validator.validateInput(null)

    widthSpinner.setValue(widthSpinner.value, false)
    heightSpinner.setValue(heightSpinner.value, false)
    layoutSpinner.setCurrent(layoutSpinner.value, false)

    if (layoutSpinner.value.gridLayoutStrategy.checkParameters(widthSpinner.value, heightSpinner.value)) {
      Hex.assets.islandPreviews.renderPreview(
        createIsland(),
        previewSize,
        previewSize,
        dummyMetadata
      ) {
        previewBuffer?.dispose()
        previewBuffer = it
        val region = TextureRegion(it.colorBufferTexture)
        region.flip(false, true)
        previewImage.drawable = TextureRegionDrawable(region)
      }
    }

    if (Hex.debugStage) {
      previewBufferNoise?.dispose()
      val size = widthSpinner.value

      if (pixmap == null) {
        pixmap = Pixmap(size * 2, size * 2, RGB565).also {
          it.blending = None
          it.filter = NearestNeighbour
        }
      }
      val pixmap = pixmap ?: return

      for (x in -size until size) {
        for (y in -size until size) {
          val noise =
            IslandGeneration.noiseAt(x.toFloat(), y.toFloat(), widthSpinner.value + 2, heightSpinner.value + 2)
          val color = if (noise <= 1) BLACK else WHITE
          pixmap.drawPixel(size + x, size + y, color.toIntBits())
        }
      }

      val buffer = FrameBuffer(RGBA8888, NOISE_SIZE, NOISE_SIZE, false)
      buffer.use {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val texture = Texture(pixmap).also { texture ->
          texture.setFilter(Nearest, Nearest)
        }

        batch.use { batch ->
          batch.draw(texture, 0f, 0f, NOISE_SIZE.toFloat(), NOISE_SIZE.toFloat())
        }
      }
      previewImageNoise?.also {
        it.drawable = TextureRegionDrawable(buffer.colorBufferTexture)
      }
    }
  }

  override fun dispose() {
    super.dispose()
    pixmap.disposeSafely()
    previewBufferNoise.disposeSafely()
    previewBuffer.disposeSafely()
    dummyMetadata.dispose()
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    renderPreview()
  }

  companion object {
    private const val NOISE_SIZE = 512
    private const val WIDTH_SPINNER_NAME = "width"
  }
}