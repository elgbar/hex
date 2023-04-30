package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.preview.IslandPreviewCollection
import no.elg.hex.util.play

/** @author Elg */
class SplashScreen(var nextScreen: AbstractScreen?) : AbstractScreen(), ReloadableScreen {

  private var startTime: Long = System.currentTimeMillis()

  private val layout by lazy { GlyphLayout() }

  init {
    check(nextScreen !== this) { "Cannot set the next screen to be this screen!" }
  }

  override fun recreate(): AbstractScreen = SplashScreen(nextScreen)

  override fun show() {
    startTime = System.currentTimeMillis()
  }

  override fun render(delta: Float) {
    if (!Hex.paused && Hex.assets.mainFinishedLoading && !IslandPreviewCollection.renderingPreviews && Hex.assets.update(10)) {
      val realNextScreen = nextScreen
      if (realNextScreen != null && realNextScreen !== this) {
        refreshAndSetScreen(realNextScreen)
      } else {
        Hex.screen = LevelSelectScreen()
      }
      Gdx.app.log("SPLASH", "All assets finished loading in ${System.currentTimeMillis() - startTime} ms")
    } else {
      batch.use {
        val txt =
          """
          |Loading ${Hex.assets.loadingInfo}
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
        Gdx.graphics.requestRendering()
      }
    }
  }

  companion object {
    fun refreshAndSetScreen(screen: AbstractScreen) {
      if (screen is OverlayScreen) {
        screen.backToPreviousScreen()
      } else if (!screen.isDisposed) {
        Hex.screen = screen
        return
      } else if (screen is PreviewIslandScreen) {
        play(screen.id, screen.island)
      } else if (screen is SplashIslandScreen) {
        play(screen.id)
      } else if (screen is ReloadableScreen) {
        Hex.screen = screen.recreate()
      } else {
        MessagesRenderer.publishError("Don't know how to restore the previous screen ${screen::class.simpleName}")
        Hex.screen = LevelSelectScreen()
      }
    }
  }
}