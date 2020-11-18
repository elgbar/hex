package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling.fit
import com.badlogic.gdx.utils.viewport.ScalingViewport
import com.kotcrab.vis.ui.util.form.FormInputValidator
import com.kotcrab.vis.ui.widget.ButtonBar.ButtonType.CANCEL
import com.kotcrab.vis.ui.widget.ButtonBar.ButtonType.OK
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.spinner.ArraySpinnerModel
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.Spinner
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.scene2d.actors
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
import no.elg.hex.island.Island.Companion.STARTING_TEAM
import no.elg.hex.util.getData
import no.elg.hex.util.play
import org.hexworks.mixite.core.api.HexagonalGridLayout
import org.hexworks.mixite.core.api.HexagonalGridLayout.HEXAGONAL
import org.hexworks.mixite.core.api.HexagonalGridLayout.RECTANGULAR
import org.hexworks.mixite.core.api.HexagonalGridLayout.TRAPEZOID
import org.hexworks.mixite.core.api.HexagonalGridLayout.TRIANGULAR
import com.badlogic.gdx.utils.Array as GdxArray

/** @author Elg */
object LevelCreationScreen : AbstractScreen() {

  val stage = Stage(ScalingViewport(fit, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), camera))

  init {
    stage.actors {
      visTable {
        defaults().pad(20f)
        setFillParent(true)
        setRound(false)
        if (Hex.debug) {
          debug()
        }
        val layoutSpinner: ArraySpinnerModel<HexagonalGridLayout> =
          ArraySpinnerModel(GdxArray(HexagonalGridLayout.values()))

        val widthSpinner = IntSpinnerModel(19, 1, Int.MAX_VALUE)
        val heightSpinner = IntSpinnerModel(19, 1, Int.MAX_VALUE)

        var previewBuffer: FrameBuffer? = null

        fun createIsland(isPreview: Boolean): Island {
          return Island(widthSpinner.value + 2, heightSpinner.value + 2, layoutSpinner.current)
            .also {
              if (isPreview) {
                for (hexagon in it.hexagons) {
                  it.getData(hexagon).team = STARTING_TEAM
                }
              }
            }
        }

        val previewSize =
          (((Gdx.graphics.width - 3 * (Gdx.graphics.width * 0.025f)) / 2).toInt() * 2)
            .coerceAtLeast(1024)

        val previewImage =
          visImage(Texture(previewSize, previewSize, Pixmap.Format.RGBA8888)) {
            it.expand()
            it.fill()
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
            this@visTable.pack()

            previewBuffer = LevelSelectScreen.renderPreview(
              createIsland(true),
              previewImage.imageWidth.toInt(),
              previewImage.imageHeight.toInt()
            ).also {
              val region = TextureRegion(it.colorBufferTexture)
              region.flip(false, true)
              previewImage.drawable = TextureRegionDrawable(region)
            }
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

              layoutSpinner.current = HEXAGONAL
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
                Gdx.app.debug(
                  "CREATOR",
                  "Creating island ${LevelSelectScreen.islandAmount} with a dimension of " +
                    "${widthSpinner.value} x ${heightSpinner.value} and layout ${layoutSpinner.current}"
                )
                play(LevelSelectScreen.islandAmount, createIsland(false))
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
  }

  override fun render(delta: Float) {
    stage.act(delta)
    stage.draw()

    if (Hex.debug) {
      lineRenderer.begin(ShapeType.Line)
      lineRenderer.color = Color.LIGHT_GRAY
      lineRenderer.line(
        Gdx.graphics.width / 2f, 0f, Gdx.graphics.width / 2f, Gdx.graphics.height.toFloat()
      )
      lineRenderer.line(
        0f, Gdx.graphics.height / 2f, Gdx.graphics.width.toFloat(), Gdx.graphics.height / 2f
      )
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
    camera.setToOrtho(false)
    stage.viewport.setWorldSize(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    stage.viewport.setScreenSize(Gdx.graphics.width, Gdx.graphics.height)
    val visTable = stage.actors.first() as VisTable
    val spinner = visTable.findActor<Spinner>(WIDTH_SPINNER_NAME)

    spinner.notifyValueChanged(true)
    visTable.pack()
  }

  const val WIDTH_SPINNER_NAME = "width"
}
