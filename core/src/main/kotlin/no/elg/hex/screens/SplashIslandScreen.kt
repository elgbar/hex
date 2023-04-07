package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.island.Island
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.trace

class SplashIslandScreen(val id: Int, private var island: Island? = null) : AbstractScreen() {

  var loadable: Boolean = true
    private set

  private var startTime: Long = System.currentTimeMillis()

  private val layout by lazy { GlyphLayout() }

  companion object {
    var loading = false
      private set
  }

  init {
    require(!loading) { "Two island splash screens should not be active at the same time!" }
    loading = true
    val islandFile = getIslandFile(id)
    val initIsland = island
    if (initIsland == null && !islandFile.exists()) {
      MessagesRenderer.publishWarning("Tried to play island $id, but no such island is loaded")
      loading = false
      loadable = false
    } else {
      Hex.screen = if (initIsland != null) {
        dispose()
        createIslandScreen(initIsland)
      } else {
        KtxAsync.launch(Hex.asyncThread) {

          try {

            val progress = PreviewIslandScreen.getProgress(id)
            Gdx.app.trace("IS SPLASH") { "progress: $progress" }
            island = if (!Hex.args.mapEditor && !progress.isNullOrBlank()) {
              Gdx.app.debug("IS SPLASH", "Found progress for island $id")
              Island.deserialize(progress)
            } else {
              Gdx.app.debug("IS SPLASH", "No progress found for island $id")
              Island.deserialize(getIslandFile(id))
            }
          } catch (e: Exception) {
            Gdx.app.postRunnable {
              MessagesRenderer.publishError(
                "Failed to load island $id due to a ${e::class.simpleName}: ${e.message}",
                exception = e
              )
              Hex.screen = LevelSelectScreen
            }
          }
        }
        this
      }
      loading = false
    }
  }

  private fun createIslandScreen(island: Island) =
    if (Hex.args.mapEditor) MapEditorScreen(id, island) else PlayableIslandScreen(id, island)

  override fun render(delta: Float) {
    val currIsland = island
    if (currIsland != null) {
      loading = false
      Hex.screen = createIslandScreen(currIsland)
      Gdx.app.log("IS SPLASH", "Loaded island $id in ${System.currentTimeMillis() - startTime} ms")
    } else {
      batch.use {
        val txt =
          """
          |Loading Island $id
          |
          |0%
          |
          |${System.currentTimeMillis() - startTime} ms
          """.trimMargin()

        layout.setText(
          Hex.assets.regularFont,
          txt,
          Color.WHITE,
          Gdx.graphics.width.toFloat(),
          Align.center,
          true
        )
        Hex.assets.regularFont.draw(batch, layout, 0f, Gdx.graphics.height.toFloat() / 2)
        Gdx.graphics.requestRendering()
      }
    }
  }

  override fun show() {
    startTime = System.currentTimeMillis()
  }
}