package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array as GdxArray
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScalingViewport
import com.kotcrab.vis.ui.util.form.FormInputValidator
import com.kotcrab.vis.ui.widget.ButtonBar.ButtonType.APPLY
import com.kotcrab.vis.ui.widget.ButtonBar.ButtonType.CANCEL
import com.kotcrab.vis.ui.widget.spinner.ArraySpinnerModel
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import ktx.actors.onChangeEvent
import ktx.actors.onClick
import ktx.scene2d.actors
import ktx.scene2d.vis.buttonBar
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.validator
import ktx.scene2d.vis.visImage
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.util.play
import org.hexworks.mixite.core.api.HexagonalGridLayout

/** @author Elg */
object LevelCreationScreen : AbstractScreen() {

  val stage =
      Stage(
          ScalingViewport(
              Scaling.fillX, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), camera))

  init {

    stage.actors {
      visTable(true) {
        validator {
          setFillParent(true)

          val layoutSpinner: ArraySpinnerModel<HexagonalGridLayout> =
              ArraySpinnerModel(GdxArray(HexagonalGridLayout.values()))
          val widthSpinner = IntSpinnerModel(10, 1, Int.MAX_VALUE)
          val heightSpinner = IntSpinnerModel(10, 1, Int.MAX_VALUE)

          var previewBuffer: FrameBuffer? = null

          fun createIsland(): Island =
              Island(widthSpinner.value + 2, heightSpinner.value + 2, layoutSpinner.current)

          val previewSize =
              ((Gdx.graphics.width - 3 * (Gdx.graphics.width * 0.025f)) / 2).toInt() * 2

          val previewImage = visImage(Texture(previewSize, previewSize, Pixmap.Format.RGBA8888)) {}

          fun renderPreview() {
            previewBuffer?.dispose()

            widthSpinner.setValue(widthSpinner.value, false)
            heightSpinner.setValue(heightSpinner.value, false)
            layoutSpinner.setCurrent(layoutSpinner.current, false)

            if (layoutSpinner.current.gridLayoutStrategy.checkParameters(
                widthSpinner.value, heightSpinner.value)) {

              previewBuffer =
                  LevelSelectScreen.renderPreview(createIsland(), previewSize).also {
                    val region = TextureRegion(it.colorBufferTexture)
                    region.flip(false, true)

                    previewImage.drawable = TextureRegionDrawable(region)
                  }
            }
          }

          Gdx.app.postRunnable { renderPreview() }
          row()

          val validator =
              object : FormInputValidator("Invalid width/height for given layout") {
                override fun validate(input: String?): Boolean {
                  return layoutSpinner.current?.gridLayoutStrategy?.checkParameters(
                      widthSpinner.value, heightSpinner.value)
                      ?: true
                }
              }

          spinner("Width", widthSpinner) {
            textField.addValidator(validator)
            onChangeEvent { renderPreview() }
          }
          spinner("Height", heightSpinner) {
            textField.addValidator(validator)
            onChangeEvent { renderPreview() }
          }
          spinner("Layout", layoutSpinner) {
            textField.addValidator(validator)
            onChangeEvent { renderPreview() }
          }

          row()
          val createButton =
              visTextButton("Create island") {
                onClick {
                  Gdx.app.debug(
                      "CREATOR",
                      "Creating island ${LevelSelectScreen.islandAmount} with a dimension of ${widthSpinner.value} x ${heightSpinner.value} and layout ${layoutSpinner.current}")
                  play(LevelSelectScreen.islandAmount, createIsland())
                }
              }
          val cancelButton = visTextButton("Cancel") { onClick { Hex.screen = LevelSelectScreen } }

          buttonBar {
            setButton(APPLY, createButton)
            setButton(CANCEL, cancelButton)
          }
          addDisableTarget(createButton)
        }
        pack()
      }
    }
  }

  override fun render(delta: Float) {
    lineRenderer.begin(ShapeType.Line)
    lineRenderer.color = Color.LIGHT_GRAY
    lineRenderer.line(
        Gdx.graphics.width / 2f, 0f, Gdx.graphics.width / 2f, Gdx.graphics.height.toFloat())
    lineRenderer.line(
        0f, Gdx.graphics.height / 2f, Gdx.graphics.width.toFloat(), Gdx.graphics.height / 2f)
    lineRenderer.end()

    stage.act(delta)
    stage.draw()
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
    stage.viewport.update(width, height, true)
  }
}
