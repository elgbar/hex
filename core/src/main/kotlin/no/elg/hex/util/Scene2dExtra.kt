package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.PopupMenu
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.isShown
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.scene2d.Scene2dDsl

/**
 * Alias for [PopupMenu.addSeparator] to make it blend better in with the scene 2d DSL, but without
 * the padding
 */
@Scene2dDsl
fun PopupMenu.separator() {
  add(Separator("menu")).fill().expand().row()
}

/**
 * Call [interaction] when either the user clicks on the menu item or when pressing all the given
 * keys.
 */
fun MenuItem.onInteract(stage: Stage, vararg keyShortcut: Int, interaction: () -> Unit) {
  if (keyShortcut.isNotEmpty()) {
    setShortcut(*keyShortcut)
    stage += onAllKeysDownEvent(*keyShortcut, catchEvent = true) { interaction() }
  }

  onClick { interaction() }
}

/** Add and fade in this window if it is is not [isShown] */
fun VisWindow.show(stage: Stage) {
  if (!isShown()) {
    stage.addActor(fadeIn())
  }
}

/** Alias for [VisWindow.fadeOut] with a fadeout duration of `0f` */
fun VisWindow.hide() {
  fadeOut(0f)
}

/** Toggle if this window is shown or not */
fun VisWindow.toggleShown(stage: Stage) {
  if (!isShown()) {
    stage.addActor(fadeIn())
  } else {
    fadeOut()
  }
}

operator fun Stage.plusAssign(eventListener: EventListener) {
  addListener(eventListener)
}

/** Call listener if a key down event where the keycode is the given key is fired */
inline fun <T : Actor> T.onKeyDown(
  keycode: Int,
  catchEvent: Boolean = false,
  crossinline listener: T.() -> Unit
) =
  onKeyDown(catchEvent) { eventKey ->
    if (eventKey == keycode) {
      listener()
    }
  }

/** Call listener when all keys are pressed and one of them are in the fired onKeyDown event */
inline fun <T : Actor> T.onAllKeysDownEvent(
  vararg keycodes: Int,
  catchEvent: Boolean = false,
  crossinline listener: T.() -> Unit
): EventListener {
  require(keycodes.isNotEmpty()) { "At least one key must be given" }
  return this.onKeyDown(catchEvent) { eventKey ->
    if (eventKey in keycodes && keycodes.all { it == eventKey || Gdx.input.isKeyPressed(it) }) {
      listener()
    }
  }
}
