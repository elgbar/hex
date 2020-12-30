package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.island.Island
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.getIslandFileName

class SplashIslandScreen(val id: Int, private var island: Island? = null) : AbstractScreen() {

  var loadable: Boolean = true
    private set

  private val assetId = getIslandFileName(id)

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
      if (!Hex.assets.isLoaded(assetId)) {
        Hex.assets.load(assetId, Island::class.java)
      }
      Gdx.app.postRunnable {
        loading = false
        Hex.screen = if (initIsland != null) {
          dispose()
          createIslandScreen(initIsland)
        } else {
          this
        }
      }
    }
  }

  private fun createIslandScreen(island: Island) =
    if (Hex.args.mapEditor) MapEditorScreen(id, island) else PlayableIslandScreen(id, island)

  override fun render(delta: Float) {

    if (Hex.assets.isLoaded(assetId)) {
      island = Hex.assets[assetId] ?: error("Asset is loaded but nothing was returned!")
    }

    Hex.assets.update(25)

    island?.also {
      loading = false
      Hex.screen = createIslandScreen(it)
      Gdx.app.log("IS SPLASH", "Loaded island $id in ${System.currentTimeMillis() - startTime} ms")
      return
    }
    batch.use {
      val txt =
        """
          |Loading Island $id
          |
          |${"%2.0f".format(Hex.assets.progress * 100)}%
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
    }
  }

  override fun show() {
    startTime = System.currentTimeMillis()
  }
}
