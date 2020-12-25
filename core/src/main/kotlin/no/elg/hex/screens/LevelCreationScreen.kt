package no.elg.hex.screens

import com.badlogic.gdx.Gdx
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
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import com.kotcrab.vis.ui.util.form.FormInputValidator
import com.kotcrab.vis.ui.widget.ButtonBar.ButtonType.CANCEL
import com.kotcrab.vis.ui.widget.ButtonBar.ButtonType.OK
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.spinner.ArraySpinnerModel
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.SimpleFloatSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.Spinner
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.scene2d.actor
import ktx.scene2d.horizontalGroup
import ktx.scene2d.scene2d
import ktx.scene2d.vis.buttonBar
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visImage
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.island.IslandFiles
import no.elg.hex.island.IslandGeneration
import no.elg.hex.island.IslandGeneration.INITIAL_FRACTAL_GAIN
import no.elg.hex.island.IslandGeneration.INITIAL_FRACTAL_LACUNARITY
import no.elg.hex.island.IslandGeneration.INITIAL_FRACTAL_OCTAVES
import no.elg.hex.island.IslandGeneration.INITIAL_FREQUENCY
import no.elg.hex.util.play
import org.hexworks.mixite.core.api.HexagonalGridLayout
import org.hexworks.mixite.core.api.HexagonalGridLayout.HEXAGONAL
import org.hexworks.mixite.core.api.HexagonalGridLayout.RECTANGULAR
import org.hexworks.mixite.core.api.HexagonalGridLayout.TRAPEZOID
import org.hexworks.mixite.core.api.HexagonalGridLayout.TRIANGULAR
import kotlin.random.Random
import com.badlogic.gdx.utils.Array as GdxArray

/** @author Elg */
object LevelCreationScreen : StageScreen() {

  const val NOISE_SIZE = 512
  const val NOISE_SIZE_F = 512f

  init {
    if (Hex.debug) {
      batch.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, NOISE_SIZE_F, NOISE_SIZE_F)
    }
  }

  init {
    rootTable {

      val layoutSpinner = ArraySpinnerModel(GdxArray(HexagonalGridLayout.values()))

      // arbitrary initial value
      val widthSpinner = IntSpinnerModel(31, 1, Int.MAX_VALUE)
      val heightSpinner = IntSpinnerModel(31, 1, Int.MAX_VALUE)
      val seedField = VisTextField(Random.nextLong().toString())

      var previewBuffer: FrameBuffer? = null
      val previewBufferNoise: FrameBuffer? = null
      var pixmap: Pixmap? = null

      fun createIsland(): Island {
        return IslandGeneration.generate(seedField.text.hashCode(), widthSpinner.value + 2, heightSpinner.value + 2, layoutSpinner.current)
      }

      val previewSize = (((Gdx.graphics.width - 3 * (Gdx.graphics.width * 0.025f)) / 2).toInt() * 2).coerceAtLeast(1024)

      val previewImage: VisImage
      var previewImageNoise: VisImage? = null
      visTable { table ->
        table.fill()
        table.expand()
        previewImage = visImage(Texture(previewSize, previewSize, RGBA8888)) {
          it.fill()
          it.expand()
          it.center()
        }

        if (Hex.args.`stage-debug`) {
          previewImageNoise = visImage(TextureRegionDrawable(Texture(NOISE_SIZE, NOISE_SIZE, RGB565)), Scaling.fillY) {
            it.fill()
            it.expand()
            it.center()
          }
        }
      }

      val disableables = mutableListOf<Disableable>()

      val validator =
        object : FormInputValidator("Invalid width/height for given layout") {
          override fun validate(input: String?): Boolean {
            val valid = layoutSpinner.current?.gridLayoutStrategy?.checkParameters(widthSpinner.value, heightSpinner.value) ?: true

            for (disableable in disableables) {
              disableable.isDisabled = !valid
            }

            return valid
          }
        }

      fun renderPreview() {
        previewBuffer?.dispose()

        validator.validateInput(null)

        widthSpinner.setValue(widthSpinner.value, false)
        heightSpinner.setValue(heightSpinner.value, false)
        layoutSpinner.setCurrent(layoutSpinner.current, false)

        if (layoutSpinner.current.gridLayoutStrategy.checkParameters(widthSpinner.value, heightSpinner.value)) {
          // force update to imageWidth and imageHeight to make sure we have the correct size
          this@rootTable.pack()

          previewBuffer = LevelSelectScreen.renderPreview(
            createIsland(),
            previewImage.imageWidth.toInt(),
            previewImage.imageHeight.toInt()
          ).also {
            val region = TextureRegion(it.colorBufferTexture)
            region.flip(false, true)
            previewImage.drawable = TextureRegionDrawable(region)
          }
        }

        if (Hex.args.`stage-debug`) {
          previewBufferNoise?.dispose()
          val size = widthSpinner.value

          if (pixmap == null) {
            pixmap = Pixmap(size * 2, size * 2, RGB565).also {
              it.blending = None
              it.filter = NearestNeighbour
            }
          }
          @Suppress("NAME_SHADOWING")
          val pixmap = pixmap ?: return

          for (x in -size until size) {
            for (y in -size until size) {
              val noise = IslandGeneration.noiseAt(x.toFloat(), y.toFloat(), widthSpinner.value + 2, heightSpinner.value + 2)
              val color = if (noise <= 1) BLACK else WHITE
              pixmap.drawPixel(size + x, size + y, color.toIntBits())
            }
          }

          val buffer = FrameBuffer(RGBA8888, NOISE_SIZE, NOISE_SIZE, false)
          buffer.begin()
          Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

          val a = Texture(pixmap).also {
            it.setFilter(Nearest, Nearest)
          }

          batch.begin()
          batch.draw(a, 0f, 0f, NOISE_SIZE_F, NOISE_SIZE_F)
          batch.end()

          buffer.end()
          previewImageNoise!!.drawable = TextureRegionDrawable(buffer.colorBufferTexture)
        }
      }

      Gdx.app.postRunnable { renderPreview() }
      row()

      var oldWidth = widthSpinner.value
      var oldHeight = heightSpinner.value

      fun syncValue(changed: IntSpinnerModel, other: IntSpinnerModel, oldValue: Int) {
        val currentValue = changed.value
        if (layoutSpinner.current == TRIANGULAR) {
          other.setValue(currentValue, false)
        } else if (layoutSpinner.current == HEXAGONAL) {
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
        spinner =
          spinner("Layout", layoutSpinner) {
            val minWidth =
              Hex.assets.fontSize *
                HexagonalGridLayout.values().maxOf { layout -> layout.name.length / 2f + 1 }
            cells.get(1)?.minWidth(minWidth)
            textField.addValidator(validator)
            onChange { renderPreview() }

//              layoutSpinner.current = HEXAGONAL
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
        when (layoutSpinner.current) {
          RECTANGULAR -> "A rectangular layout has no special rules."
          HEXAGONAL -> "The hexagonal layout must have equal width and height and it must be odd."
          TRAPEZOID -> "A trapezoid layout has no special rules."
          TRIANGULAR -> "A triangular layout must have equal width and height."
          else -> "Invalid layout: ${layoutSpinner.current}"
        }

      visLabel(layoutExplanation()) { spinner.onChange { setText(layoutExplanation()) } }

      row()

      buttonBar {
        setButton(
          OK,
          scene2d.visTextButton("Create island") {
            disableables.add(this)

            onClick {
              if (this.isDisabled) return@onClick
              val nextId = IslandFiles.nextIslandId
              Gdx.app.debug(
                "CREATOR",
                "Creating island $nextId with a dimension of " + "${widthSpinner.value} x ${heightSpinner.value} and layout ${layoutSpinner.current}"
              )
              play(nextId, createIsland())
            }
          }
        )

        setButton(
          CANCEL,
          scene2d.visTextButton("Cancel") { onClick { Hex.screen = LevelSelectScreen } }
        )

        createTable().pack()
      }

      pack()
    }
  }

  override fun render(delta: Float) {
    stage.act(delta)
    stage.draw()

    if (Hex.args.`stage-debug` || Hex.trace) {
      lineRenderer.begin(ShapeType.Line)
      lineRenderer.color = Color.LIGHT_GRAY
      lineRenderer.line(Gdx.graphics.width / 2f, 0f, Gdx.graphics.width / 2f, Gdx.graphics.height.toFloat())
      lineRenderer.line(0f, Gdx.graphics.height / 2f, Gdx.graphics.width.toFloat(), Gdx.graphics.height / 2f)
      lineRenderer.end()
    }
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(stage)
  }

  override fun hide() {
    Hex.inputMultiplexer.removeProcessor(stage)
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    val visTable = stage.actors.first() as VisTable
    val spinner = visTable.findActor<Spinner>(WIDTH_SPINNER_NAME)

    spinner.notifyValueChanged(true)
    visTable.pack()
  }

  const val WIDTH_SPINNER_NAME = "width"
}
