package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Align
import ktx.actors.isShown
import ktx.scene2d.actors
import ktx.scene2d.vis.KVisWindow
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.input.LevelSelectInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.util.clearIslandProgress
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4
import no.elg.hex.util.confirmWindow
import no.elg.hex.util.play
import no.elg.hex.util.safeUse
import no.elg.hex.util.show

/** @author Elg */
class LevelSelectScreen : AbstractScreen(), ReloadableScreen {

  private val stageScreen by lazy { StageScreen() }
  val stage: Stage get() = stageScreen.stage
  private val layout by lazy { GlyphLayout() }

  internal val input by lazy { LevelSelectInputProcessor(this) }
  private val mouseX get() = input.mouseX
  private val mouseY get() = input.mouseY

  private lateinit var confirmWindow: KVisWindow
  private var toPlay: FastIslandMetadata? = null

  val confirmingRestartIsland get() = confirmWindow.isShown()
  fun confirmRestartIsland(island: FastIslandMetadata) {
    toPlay = island
    confirmWindow.show(stage)
  }

  private fun initStage() {
    stage.actors {
      confirmWindow = confirmWindow(
        "Restart Island?",
        "This will reset all progress on the island.",
        whenConfirmed = {
          toPlay?.let {
            toPlay = null
            clearIslandProgress(it)
            play(it)
          } ?: MessagesRenderer.publishError("Failed to restart island, try disabling the 'Confirm Restart Island' setting")
        }
      )
      confirmWindow.pack()
    }
  }

  override fun render(delta: Float) {
    if (LevelSelectInputProcessor.lastY != camera.position.y) {
      input.restoreScrollPosition()
    }
    lineRenderer.safeUse(Line) {
      batch.safeUse {
        val (_, sy, _, sheight) = input.slotRect(0, NON_ISLAND_SCALE, 0f)

        // Draw the first row of non-islands
        if (sy + sheight > camera.position.y - camera.viewportHeight / 2f) {
          drawScreenSprite(Hex.assets.settingsDown, Hex.assets.settings, 0)
          drawScreenSprite(Hex.music.iconSelected, Hex.music.icon, PREVIEWS_PER_ROW - 2)
          drawScreenSprite(Hex.assets.helpDown, Hex.assets.help, PREVIEWS_PER_ROW - 1)

          if (Hex.mapEditor) {
            drawLevelCreationIcon()
          }
        }

        for ((i, metadata) in Hex.assets.islandPreviews.islandWithIndex()) {
          val (x, y, width, height) = input.slotRect(i + PREVIEWS_PER_ROW)

          if (y + height < camera.position.y - camera.viewportHeight / 2f) {
            // island is above camera, no need to render
            continue
          }

          if (y > camera.position.y + camera.viewportHeight / 2f) {
            // the island is below the camera, no need to render further
            break
          }

          val preview = metadata.preview ?: continue
          batch.draw(preview, x, y, width, height)
          if (Hex.debug || Hex.mapEditor) {
            val color = when {
              metadata.forTesting -> Color.RED
              metadata.authorRoundsToBeat == Island.NEVER_PLAYED -> Color.MAGENTA
              metadata.authorRoundsToBeat == Island.NEVER_BEATEN -> Color.YELLOW
              metadata.authorRoundsToBeat == Int.MAX_VALUE -> Color.FOREST
              Hex.debugStage -> NOT_SELECTED_COLOR
              else -> null
            }
            color?.let { drawBox(x, y, width, height, it) }
          }
          val font = Hex.assets.regularItalicFont
          val vertOffset = font.lineHeight

          val showIslandId = Hex.mapEditor || Hex.debug || Settings.showIslandId
          if (showIslandId) {
            layout.setText(font, "Island ${metadata.id}", Color.WHITE, width, Align.left, true)
            font.draw(batch, layout, x, y + vertOffset)
          }

          if (metadata.userRoundsToBeat != Island.NEVER_PLAYED) {
            val color = if (metadata.isUserBetterThanAuthor()) Color.GOLD else Color.WHITE
            val align = if (showIslandId) Align.right else Align.center
            layout.setText(font, "Best: ${metadata.userRoundsToBeat} rounds", color, width, align, true)
            font.draw(batch, layout, x, y + vertOffset)
          }

          if (Hex.mapEditor || Hex.debug) {
            layout.setText(font, "ARtB ${metadata.authorRoundsToBeat}", Color.WHITE, width, Align.left, true)
            font.draw(batch, layout, x, y + vertOffset * 2)
          }
        }
      }
    }
    stageScreen.render(delta)
  }

  fun color(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    notSelectedColor: Color = NOT_SELECTED_COLOR
  ): Color {
    return if (mouseX in x..x + width && mouseY in y..y + height) SELECT_COLOR else notSelectedColor
  }

  private fun drawBox(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    notSelectedColor: Color = NOT_SELECTED_COLOR
  ) {
    lineRenderer.color = color(x, y, width, height, notSelectedColor)
    lineRenderer.rect(x, y, width, height)
  }

  private fun drawSlotBox(index: Int) {
    val (inputX, inputY, inputWidth, inputHeight) = input.slotRect(index, 1f)
    drawBox(inputX, inputY, inputWidth, inputHeight, Color.GRAY)
  }

  private fun drawScreenSprite(selected: TextureRegion, notSelected: TextureRegion, index: Int) {
    val (x, y, width, height) = input.slotRect(index, NON_ISLAND_SCALE)
    val sprite = if (!input.ignoreInput && mouseX in x..x + width && mouseY in y..y + height) selected else notSelected
    batch.draw(sprite, x, y, width, height)
    if (Hex.debugStage) {
      // Show the interaction area of these buttons
      drawSlotBox(index)
    }
  }

  private fun drawLevelCreationIcon() {
    val newMapIndex = 1
    val (x, y, width, height) = input.slotRect(newMapIndex, NON_ISLAND_SCALE)

    drawBox(x, y, width, height)
    // Draw crosshair in the center of the box
    val color = color(x, y, width, height)
    lineRenderer.line(
      x + width / 2f,
      y + height / 2f + height / 10f,
      x + width / 2f,
      y + height / 2f - height / 10f,
      color,
      color
    )
    lineRenderer.line(
      x + width / 2f + width / 10f,
      y + height / 2f,
      x + width / 2f - width / 10f,
      y + height / 2f,
      color,
      color
    )

    if (Hex.debugStage) {
      drawSlotBox(newMapIndex)
    }
  }

  override fun recreate(): AbstractScreen {
    return LevelSelectScreen()
  }

  override fun show() {
    input.show()
    initStage()
    stageScreen.show()
    Hex.assets.songs.firstOrNull().also { song -> Hex.music.loop(song) }
  }

  override fun hide() {
    super.hide()
    stageScreen.hide()
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    input.restoreScrollPosition()
    stageScreen.resize(width, height)
  }

  companion object {
    const val NON_ISLAND_SCALE = 0.45f
    const val PREVIEWS_PER_ROW = 4 // We currently use three slots, don't decrease to two :p
    private val NOT_SELECTED_COLOR: Color = Color.LIGHT_GRAY
    private val SELECT_COLOR: Color = Color.GREEN
    private const val PREVIEW_PADDING_PERCENT = 0.025f

    val paddingX: Float
      get() = Gdx.graphics.width * PREVIEW_PADDING_PERCENT
    val paddingY: Float get() = paddingX * 1.5f
    val shownPreviewSize
      get() = (Gdx.graphics.width - (1 + PREVIEWS_PER_ROW) * paddingX) / PREVIEWS_PER_ROW
  }
}