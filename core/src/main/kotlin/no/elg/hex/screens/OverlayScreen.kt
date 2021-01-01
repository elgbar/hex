package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.vis.KVisTextButton
import ktx.scene2d.vis.visTextButton
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.util.onInteract

abstract class OverlayScreen(useRootTable: Boolean = true) : StageScreen(useRootTable) {

  private lateinit var previousScreen: AbstractScreen

  fun backToPreviousScreen() {
    if (!::previousScreen.isInitialized) {
      MessagesRenderer.publishError("Previous screen is not initialized")
      Hex.screen = LevelSelectScreen
      return
    }

    SplashScreen.refreshAndSetScreen(previousScreen)
  }

  @Scene2dDsl
  protected fun <S> KWidget<S>.addBackButton(init: KVisTextButton.(S) -> Unit = {}) {
    visTextButton("Back") {
      onInteract(stage, intArrayOf(Keys.ESCAPE), intArrayOf(Keys.BACK)) {
        backToPreviousScreen()
      }
      init(it)
    }
  }

  override fun show() {
    super.show()
    previousScreen = Hex.screen
    Gdx.app.debug("SETTINGS", "Previous screen is ${previousScreen::class.simpleName}")
    if (previousScreen === this) {
      MessagesRenderer.publishError("Previous screen cannot be this Setting screen")
      return
    }
  }
}