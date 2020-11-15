package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys.A
import com.badlogic.gdx.Input.Keys.DOWN
import com.badlogic.gdx.Input.Keys.PAGE_DOWN
import com.badlogic.gdx.Input.Keys.PAGE_UP
import com.badlogic.gdx.Input.Keys.Q
import com.badlogic.gdx.Input.Keys.S
import com.badlogic.gdx.Input.Keys.SHIFT_LEFT
import com.badlogic.gdx.Input.Keys.SHIFT_RIGHT
import com.badlogic.gdx.Input.Keys.UP
import com.badlogic.gdx.Input.Keys.W
import com.badlogic.gdx.InputAdapter
import no.elg.hex.hexagon.PIECES
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Team
import no.elg.hex.hud.MessagesRenderer.publishError
import no.elg.hex.input.editor.Editor
import no.elg.hex.input.editor.NOOPEditor
import no.elg.hex.screens.MapEditorScreen
import no.elg.hex.screens.MapEditorScreen.Companion.MAX_BRUSH_SIZE
import no.elg.hex.screens.MapEditorScreen.Companion.MIN_BRUSH_SIZE
import no.elg.hex.util.findHexagonsWithinRadius
import no.elg.hex.util.next
import no.elg.hex.util.previous
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

/** @author Elg */
class MapEditorInputProcessor(private val mapEditorScreen: MapEditorScreen) : InputAdapter() {

  var brushRadius: Int = 1
    private set

  var selectedTeam: Team = Team.values().first()
    private set

  var selectedPiece: KClass<out Piece> = PIECES.first()
    private set

  var editors: List<Editor> = emptyList()
    internal set(value) {
      field = value
      editor = value.firstOrNull() ?: NOOPEditor
    }

  var editor: Editor = NOOPEditor
    internal set(value) {
      if (value == NOOPEditor || value in editors) {
        field = value
      } else {
        field = NOOPEditor
        publishError("Wrong editor type given: $value. Expected one of $editors or $NOOPEditor")
      }
    }

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (button == Buttons.LEFT) {
      val cursorHex = mapEditorScreen.basicIslandInputProcessor.cursorHex ?: return true

      if (isShiftPressed()) {
        for (hexagon in mapEditorScreen.island.findHexagonsWithinRadius(cursorHex, brushRadius)) {
          editor.edit(hexagon)
        }
      } else {
        editor.edit(cursorHex)
      }
      return true
    }
    return false
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      W, PAGE_UP, UP -> brushRadius = min(brushRadius + 1, MAX_BRUSH_SIZE)
      S, PAGE_DOWN, DOWN -> brushRadius = max(brushRadius - 1, MIN_BRUSH_SIZE)
      Q ->
        selectedTeam =
          Team.values().let {
            if (isShiftPressed()) it.previous(selectedTeam)!! else it.next(selectedTeam)
          }
      A ->
        selectedPiece =
          PIECES.let {
            if (isShiftPressed()) it.previous(selectedPiece)!! else it.next(selectedPiece)
          }
      else -> return false
    }
    return true
  }

  private fun isShiftPressed(): Boolean =
    Gdx.input.isKeyPressed(SHIFT_LEFT) || Gdx.input.isKeyPressed(SHIFT_RIGHT)
}
