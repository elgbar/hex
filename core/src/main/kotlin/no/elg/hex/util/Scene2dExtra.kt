package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
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
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.StageWidget
import ktx.scene2d.table
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visWindow
import no.elg.hex.Hex
import no.elg.hex.platform.PlatformType.DESKTOP
import no.elg.hex.platform.PlatformType.MOBILE

val buttonPadding: Float
  get() = when (Hex.platform.type) {
    MOBILE -> 45f
    DESKTOP -> 10f
  }
val platformSpacing: Float
  get() = when (Hex.platform.type) {
    MOBILE -> 20f
    DESKTOP -> 5f
  }
val platformCheckBoxSize: Float
  get() = when (Hex.platform.type) {
    MOBILE -> 50f
    DESKTOP -> 20f
  }

/**
 * Alias for [PopupMenu.addSeparator] to make it blend better in with the scene 2d DSL, but without
 * the padding
 */
@Scene2dDsl
fun PopupMenu.separator() {
  add(Separator("menu")).fill().expand().row()
}

fun Button.onInteract(
  stage: Stage,
  vararg keyShortcut: Int,
  catchEvent: Boolean = false,
  interaction: Button.() -> Unit
) {
  this.onInteract(
    stage = stage,
    keyShortcuts = arrayOf(keyShortcut),
    catchEvent = catchEvent,
    interaction = interaction
  )
}

/**
 * Call [interaction] when either the user clicks on the menu item or when pressing all the given
 * keys.
 */
fun Button.onInteract(
  stage: Stage,
  vararg keyShortcuts: IntArray,
  catchEvent: Boolean = false,
  interaction: Button.() -> Unit
) {
  onChange(interaction)

  if (keyShortcuts.isNotEmpty()) {
    if (this is MenuItem) {
      val first = keyShortcuts.firstOrNull { it.isNotEmpty() } ?: return
      setShortcut(*first)
    }
    for (keyShortcut in keyShortcuts) {
      if (keyShortcut.isEmpty()) continue
      stage += onAllKeysDownEvent(
        *keyShortcut,
        catchEvent = catchEvent,
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
  onlyWhenShown: Boolean = false,
  crossinline listener: T.() -> Unit
) =
  onKeyDown(catchEvent) { eventKey ->
    if (eventKey == keycode && (!onlyWhenShown || isShown())) {
      listener()
    }
  }

/** Call listener when all of the given keys are pressed and one of them are in the fired onKeyDown event */
inline fun <T : Actor> T.onAllKeysDownEvent(
  vararg keycodes: Int,
  catchEvent: Boolean = false,
  onlyWhenShown: Boolean = false,
  crossinline listener: T.() -> Unit
): EventListener {
  require(keycodes.isNotEmpty()) { "At least one key must be given" }
  return this.onKeyDown(catchEvent) { eventKey ->
    if ((!onlyWhenShown || isShown()) && eventKey in keycodes && keycodes.all {
        it == eventKey || Gdx.input.isKeyPressed(
          it
        )
      }
    ) {
      listener()
    }
  }
}

/** Call listener when any of the given keys are pressed */
inline fun <T : Actor> T.onAnyKeysDownEvent(
  vararg keycodes: Int,
  catchEvent: Boolean = false,
  onlyWhenShown: Boolean = false,
  crossinline listener: T.() -> Unit
): EventListener {
  require(keycodes.isNotEmpty()) { "At least one key must be given" }
  return this.onKeyDown(catchEvent) { eventKey ->
    if ((!onlyWhenShown || isShown()) && eventKey in keycodes) {
      listener()
    }
  }
}

@Scene2dDsl
fun StageWidget.confirmWindow(
  title: String,
  text: String,
  whenDenied: KVisWindow.() -> Unit = {},
  whenConfirmed: KVisWindow.() -> Unit
): KVisWindow {
  return this.visWindow(title) {
    isMovable = false
    isModal = true
    hide()

    visLabel(text)
    row()

    table { cell ->

      cell.fillX()
      cell.expandX()
      cell.space(10f)
      cell.pad(platformSpacing)

      row()

      visLabel("") {
        it.expandX()
        it.center()
      }

      visTextButton("Yes") {
        pad(buttonPadding)
        it.expandX()
        it.center()
        onClick {
          this@visWindow.whenConfirmed()
          this@visWindow.fadeOut()
        }
      }
      visLabel("") {
        it.expandX()
        it.center()
      }

      visTextButton("No") {
        pad(buttonPadding)
        it.expandX()
        it.center()
        onClick {
          this@visWindow.whenDenied()
          this@visWindow.fadeOut()
        }
      }
      visLabel("") {
        it.expandX()
        it.center()
      }
    }
    centerWindow()
    onAnyKeysDownEvent(Input.Keys.ESCAPE, Input.Keys.BACK, catchEvent = true) {
      this@visWindow.fadeOut()
    }
    pack()
    fadeOut(0f)
  }
}

@Scene2dDsl
fun StageWidget.okWindow(
  title: String,
  labelUpdater: MutableMap<KVisWindow, KVisWindow.() -> Unit>,
  whenConfirmed: KVisWindow.() -> Unit,
  text: () -> String
): VisWindow {
  return visWindow(title) {
    isMovable = false
    isModal = true
    this.hide()
    val label = visLabel("")

    labelUpdater[this] = {
      label.setText(text())
      pack()
      centerWindow()
    }

    row()

    visTextButton("OK") {
      this.pad(buttonPadding)
      it.expandX()
      it.center()
      it.space(10f)
      it.pad(platformSpacing)
      onClick {
        this@visWindow.whenConfirmed()
        this@visWindow.fadeOut()
      }
    }

    pack()
    centerWindow()
    fadeOut(0f)
  }
}