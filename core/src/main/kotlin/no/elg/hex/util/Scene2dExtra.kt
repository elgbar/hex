package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener
import com.kotcrab.vis.ui.util.InputValidator
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
import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.separator
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import ktx.scene2d.vis.visValidatableTextField
import ktx.scene2d.vis.visWindow
import no.elg.hex.Hex
import no.elg.hex.platform.PlatformType.DESKTOP
import no.elg.hex.platform.PlatformType.MOBILE

val platformButtonPadding: Float
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

@Scene2dDsl
fun KVisTable.separator(init: (@Scene2dDsl Separator).(Cell<*>) -> Unit = {}): Separator {
  val table = this@separator
  return table.separator("menu") {
    it.expandX()
    it.fillX()
    it.colspan(table.columns)
    this.init(it)
  }.also { row() }
}

@Scene2dDsl
fun padAndSpace(cell: Cell<*>) {
  cell.pad(platformButtonPadding)
  cell.space(platformSpacing)
}

/**
 * Alias for [PopupMenu.addSeparator] to make it blend better in with the scene 2d DSL, but without
 * the padding
 */
@Scene2dDsl
fun PopupMenu.separator() {
  add(Separator("menu")).fill().expand().row()
}

/**
 * Call [interaction] when either the user clicks on the menu item or when pressing **all** the given
 * keys.
 */
fun Button.onInteract(
  stage: Stage,
  vararg allPressedKeyShortcuts: Int,
  playClick: Boolean = true,
  catchEvent: Boolean = false,
  createTooltip: Boolean = true,
  interaction: Button.() -> Unit
) {
  this.onInteract(
    stage = stage,
    allPressedKeyShortcuts = arrayOf(allPressedKeyShortcuts),
    playClick = playClick,
    catchEvent = catchEvent,
    createTooltip = createTooltip,
    interaction = interaction
  )
}

/**
 * Call [interaction] when either the user clicks on the menu item or when pressing **all** the given
 * keys.
 */
fun Button.onInteract(
  stage: Stage,
  vararg allPressedKeyShortcuts: IntArray,
  catchEvent: Boolean = false,
  playClick: Boolean = true,
  createTooltip: Boolean = true,
  interaction: Button.() -> Unit
) {
  val interactionWithSound: Button.() -> Unit = {
    if (playClick) {
      playClick()
    }
    interaction()
  }
  onChange(interactionWithSound)
  if (createTooltip) {
    visTextTooltip("Shortcut: ${allPressedKeyShortcuts.filter { it.isNotEmpty() }.joinToString(" / ") { it.joinToString("+") { Input.Keys.toString(it) } }}")
  }

  if (allPressedKeyShortcuts.isNotEmpty()) {
    if (this is MenuItem) {
      val first = allPressedKeyShortcuts.firstOrNull { it.isNotEmpty() } ?: return
      setShortcut(*first)
    }
    for (keyShortcut in allPressedKeyShortcuts) {
      if (keyShortcut.isEmpty()) continue
      stage += onAllKeysDownEvent(
        *keyShortcut,
        catchEvent = catchEvent,
        listener = {
          if (!isDisabled) {
            interactionWithSound()
          }
        }
      )
    }
  }
}

/** Add and fade in this window if it is not [isShown] */
fun VisWindow.show(stage: Stage, center: Boolean = true, fadeTime: Float = FADE_TIME) {
  if (!isShown()) {
    stage.addActor(this)
    setColor(1f, 1f, 1f, 0f)
    Gdx.graphics.isContinuousRendering = true
    val actions = Actions.sequence(
      Actions.fadeIn(fadeTime, Interpolation.fade),
      Actions.run { Gdx.graphics.isContinuousRendering = false }
    )
    addAction(actions)
    if (center) {
      centerWindow()
    }
  } else {
    Gdx.app.debug("WINDOWS") { "Tried to show '$titleLabel' but it is already shown" }
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
inline fun <T : Actor> T.onKeyDown(keycode: Int, catchEvent: Boolean = false, onlyWhenShown: Boolean = false, crossinline listener: T.() -> Unit) =
  onKeyDown(catchEvent) { eventKey ->
    if (eventKey == keycode && (!onlyWhenShown || isShown())) {
      listener()
    }
  }

/** Call listener when all of the given keys are pressed and one of them are in the fired onKeyDown event */
inline fun <T : Actor> T.onAllKeysDownEvent(vararg keycodes: Int, catchEvent: Boolean = false, onlyWhenShown: Boolean = false, crossinline listener: T.() -> Unit): EventListener {
  require(keycodes.isNotEmpty()) { "At least one key must be given" }
  return this.onKeyDown(catchEvent) { eventKey ->
    if ((!onlyWhenShown || isShown()) &&
      eventKey in keycodes &&
      keycodes.all {
        it == eventKey ||
          Gdx.input.isKeyPressed(
            it
          )
      }
    ) {
      listener()
    }
  }
}

/** Call listener when any of the given keys are pressed */
inline fun <T : Actor> T.onAnyKeysDownEvent(vararg keycodes: Int, catchEvent: Boolean = false, onlyWhenShown: Boolean = false, crossinline listener: T.() -> Unit): EventListener {
  require(keycodes.isNotEmpty()) { "At least one key must be given" }
  return this.onKeyDown(catchEvent) { eventKey ->
    if ((!onlyWhenShown || isShown()) && eventKey in keycodes) {
      listener()
    }
  }
}

@Scene2dDsl
fun StageWidget.confirmWindow(title: String, text: String, whenDenied: KVisWindow.() -> Unit = {}, whenConfirmed: KVisWindow.() -> Unit): KVisWindow =
  this.visWindow(title) {
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
        pad(platformButtonPadding)
        it.expandX()
        it.center()
        onClick {
          playClick()
          this@visWindow.whenConfirmed()
          this@visWindow.fadeOut()
        }
      }
      visLabel("") {
        it.expandX()
        it.center()
      }

      visTextButton("No") {
        pad(platformButtonPadding)
        it.expandX()
        it.center()
        onClick {
          playClick()
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
      this@visWindow.whenDenied()
      this@visWindow.fadeOut()
    }
    pack()
    fadeOut(0f)
  }

@Scene2dDsl
fun StageWidget.okWindow(title: String, labelUpdater: MutableMap<KVisWindow, KVisWindow.() -> Unit>, whenConfirmed: KVisWindow.() -> Unit, text: () -> String): KVisWindow =
  visWindow(title) {
    isMovable = false
    isModal = true
    val label = visLabel("")

    labelUpdater[this] = {
      label.setText(text())
      pack()
      centerWindow()
    }

    row()

    visTextButton("OK") {
      this.pad(platformButtonPadding)
      it.expandX()
      it.center()
      it.space(10f)
      it.pad(platformSpacing)
      onClick {
        playClick()
        this@visWindow.whenConfirmed()
        this@visWindow.fadeOut()
      }
    }

    pack()
    centerWindow()
    hide()
  }

@Scene2dDsl
fun StageWidget.inputWindow(
  title: String,
  prompt: String,
  buttonText: String = "Load",
  inputValidator: InputValidator,
  whenConfirmed: KVisWindow.(String) -> Unit
): KVisWindow =
  this.visWindow(title) {
    isMovable = false
    isModal = true
    hide()

    visLabel(prompt)
    row()

    table { cell ->

      cell.fillX()
      cell.expandX()
      cell.space(10f)
      cell.pad(platformSpacing)

      row()

      val textInputField = visValidatableTextField("") {
        it.expandX()
        it.center()
        addValidator(inputValidator)
      }

      visTextButton(buttonText) {
        pad(platformButtonPadding)
        it.expandX()
        it.center()
        this@visTextButton.isDisabled = true
        textInputField.onChange {
          this@visTextButton.isDisabled = !textInputField.isInputValid
        }
        onInteract(stage, intArrayOf(Keys.ENTER), intArrayOf(Keys.NUMPAD_ENTER)) {
          if (textInputField.isInputValid) {
            this@visWindow.whenConfirmed(textInputField.text)
            this@visWindow.fadeOut()
          }
        }
      }

      stage.addListener { event ->
        if (event is FocusListener.FocusEvent && event.target == this@visWindow) {
          event.stop()
          event.cancel()

          Gdx.app.postRunnable {
            textInputField.focusField()
          }
        }
        false
      }
    }
    centerWindow()
    onAnyKeysDownEvent(Input.Keys.ESCAPE, Input.Keys.BACK, catchEvent = true) {
      this@visWindow.fadeOut()
    }
    pack()
    fadeOut(0f)
  }