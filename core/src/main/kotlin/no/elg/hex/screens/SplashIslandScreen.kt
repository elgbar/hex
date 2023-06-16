package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.input.AbstractInput
import no.elg.hex.island.Island
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.loadIslandSync
import no.elg.hex.util.playClick

class SplashIslandScreen(val id: Int, var island: Island? = null) : AbstractScreen() {

  var loadable: Boolean = true
    private set

  private var startTime: Long = System.currentTimeMillis()

  private val layout by lazy { GlyphLayout() }

  init {
    require(!loading) { "Two island splash screens should not be active at the same time!" }
    loading = true
    val islandFile = getIslandFile(id)
    val initIsland = island
    if (initIsland == null && !islandFile.exists()) {
      MessagesRenderer.publishWarning("Tried to play island $id, but island does not exist")
      loading = false
      loadable = false
    } else {
      Hex.screen = if (initIsland != null) {
        dispose()
        createIslandScreen(id, initIsland)
      } else {
        KtxAsync.launch(Hex.asyncThread) {
          island = loadIslandSync(id)
        }
        this
      }
      loading = false
    }
  }

  override fun render(delta: Float) {
    val currIsland = island
    if (currIsland != null) {
      loading = false
      Hex.screen = createIslandScreen(id, currIsland)
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
    SplashIslandInputProcessor.show()
  }

  companion object {
    var loading = false
      private set

    object SplashIslandInputProcessor : AbstractInput() {
      override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
          Input.Keys.ESCAPE, Input.Keys.BACK -> {
            Hex.screen = LevelSelectScreen()
            playClick()
          }

          else -> return false
        }
        return true
      }
    }

    fun createIslandScreen(id: Int, island: Island, center: Boolean = true) =
      (if (Hex.args.mapEditor) MapEditorScreen(id, island) else PlayableIslandScreen(id, island)).also {
//        if (center) {
//          it.resize(Gdx.graphics.width, Gdx.graphics.height)
//          it.centerCamera()
//        }
      }
  }
}