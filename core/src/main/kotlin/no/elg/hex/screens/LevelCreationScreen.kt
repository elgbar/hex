package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Value.minWidth
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array as GdxArray
import com.badlogic.gdx.utils.Scaling.fit
import com.badlogic.gdx.utils.viewport.ScalingViewport
import com.kotcrab.vis.ui.util.form.FormInputValidator
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.spinner.ArraySpinnerModel
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import ktx.actors.onChangeEvent
import ktx.actors.onClick
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.validator
import ktx.scene2d.vis.visImage
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.hex.Assets
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.island.Island.Companion.STARTING_TEAM
import no.elg.hex.util.getData
import no.elg.hex.util.play
import org.hexworks.mixite.core.api.HexagonalGridLayout

/** @author Elg */
object LevelCreationScreen : AbstractScreen() {

  val stage =
      Stage(
          ScalingViewport(fit, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), camera))

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
        val widthSpinner = IntSpinnerModel(10, 1, Int.MAX_VALUE)
        val heightSpinner = IntSpinnerModel(10, 1, Int.MAX_VALUE)

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
            visImage(Texture(previewSize, previewSize, Pixmap.Format.RGBA8888)) { it.expandX() }

        val disableables = mutableListOf<Disableable>()

        val validator =
            object : FormInputValidator("Invalid width/height for given layout") {
              override fun validate(input: String?): Boolean {
                val valid =
                    layoutSpinner.current?.gridLayoutStrategy?.checkParameters(
                        widthSpinner.value, heightSpinner.value)
                        ?: true

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

          if (layoutSpinner.current.gridLayoutStrategy.checkParameters(
              widthSpinner.value, heightSpinner.value)) {

            previewBuffer =
                LevelSelectScreen.renderPreview(createIsland(true), previewSize).also {
                  val region = TextureRegion(it.colorBufferTexture)
                  region.flip(false, true)
                  previewImage.drawable = TextureRegionDrawable(region)
                  previewImage.inCell.fill(previewImage.height / previewImage.width, 0f)
                  this@visTable.pack()
                }
          }
        }

        Gdx.app.postRunnable { renderPreview() }
        row()

        horizontalGroup {
          space(20f)

          spinner("Width", widthSpinner) {
            textField.addValidator(validator)
            onChangeEvent { renderPreview() }
          }
          spinner("Height", heightSpinner) {
            textField.addValidator(validator)
            onChangeEvent { renderPreview() }
          }
          spinner("Layout", layoutSpinner) {
            cells
                .get(1)
                ?.minWidth(
                    Assets.fontSize *
                        HexagonalGridLayout.values().maxOf { layout ->
                          layout.name.length / 2f + 1
                        })
            textField.addValidator(validator)
            onChangeEvent { renderPreview() }
          }
        }

        row()

        horizontalGroup {
          space(20f)

          visTextButton("Create island") {
            disableables.add(this)

            onClick {
              if (this.isDisabled) return@onClick
              Gdx.app.debug(
                  "CREATOR",
                  "Creating island ${LevelSelectScreen.islandAmount} with a dimension of ${widthSpinner.value} x ${heightSpinner.value} and layout ${layoutSpinner.current}")
              play(LevelSelectScreen.islandAmount, createIsland(false))
            }
          }

          visTextButton("Cancel") { onClick { Hex.screen = LevelSelectScreen } }
        }

        pack()
      }
    }
  }

  val inputListener =
      object : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
          when (keycode) {
            Keys.ESCAPE -> Hex.screen = LevelSelectScreen
            else -> return false
          }
          return true
        }
      }

  override fun render(delta: Float) {
    stage.act(delta)
    stage.draw()

    if (Hex.debug) {
      lineRenderer.begin(ShapeType.Line)
      lineRenderer.color = Color.LIGHT_GRAY
      lineRenderer.line(
          Gdx.graphics.width / 2f, 0f, Gdx.graphics.width / 2f, Gdx.graphics.height.toFloat())
      lineRenderer.line(
          0f, Gdx.graphics.height / 2f, Gdx.graphics.width.toFloat(), Gdx.graphics.height / 2f)
      lineRenderer.end()
    }
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(inputListener)
    Hex.inputMultiplexer.addProcessor(stage)
  }

  override fun hide() {
    Hex.inputMultiplexer.removeProcessor(inputListener)
    Hex.inputMultiplexer.removeProcessor(stage)
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    camera.setToOrtho(false)
    stage.viewport.setWorldSize(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    stage.viewport.setScreenSize(Gdx.graphics.width, Gdx.graphics.height)
    (stage.actors.first() as VisTable).pack()
  }
}
