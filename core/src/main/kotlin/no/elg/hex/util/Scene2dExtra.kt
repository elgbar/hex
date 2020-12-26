package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.PopupMenu
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisWindow
import com.kotcrab.vis.ui.widget.VisWindow.FADE_TIME
import ktx.actors.isShown
import ktx.actors.onChange
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

@Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
fun Button.onInteract(stage: Stage, vararg keyShortcut: Int, interaction: Button.() -> Unit) {
  this.onInteract(stage = stage, keyShortcuts = arrayOf(keyShortcut), interaction = interaction)
}

/**
 * Call [interaction] when either the user clicks on the menu item or when pressing all the given
 * keys.
 */
fun Button.onInteract(stage: Stage, vararg keyShortcuts: IntArray, interaction: Button.() -> Unit) {
  onChange(interaction)

  if (keyShortcuts.isNotEmpty()) {
    if (this is MenuItem) {
      val first = keyShortcuts.firstOrNull { it.isNotEmpty() } ?: return
      setShortcut(*first)
    }
    for (keyShortcut in keyShortcuts) {
      if (keyShortcut.isEmpty()) continue
      stage += onAllKeysDownEvent(
        *keyShortcut, catchEvent = false,
        listener = {
          if (!isDisabled) {
            interaction()
          }
        }
      )
    }
  }
}

/** Add and fade in this window if it is is not [isShown] */
fun VisWindow.show(stage: Stage, center: Boolean = true, fadeTime: Float = FADE_TIME) {
  if (!isShown()) {
    stage.addActor(fadeIn(fadeTime))
    if (center) {
      centerWindow()
    }
  }
}

/** Alias for [VisWindow.fadeOut] with a default fadeout duration of `0f` */
fun VisWindow.hide(fadeTime: Float = 0f) {
  fadeOut(fadeTime)
}

/** Toggle if this window is shown or not */
fun VisWindow.toggleShown(stage: Stage, center: Boolean = true) {
  if (!isShown()) {
    show(stage, center)
  } else {
    hide()
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
