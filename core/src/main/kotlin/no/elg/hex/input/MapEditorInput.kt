package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys.C
import com.badlogic.gdx.Input.Keys.CONTROL_LEFT
import com.badlogic.gdx.Input.Keys.CONTROL_RIGHT
import com.badlogic.gdx.Input.Keys.DOWN
import com.badlogic.gdx.Input.Keys.F1
import com.badlogic.gdx.Input.Keys.F5
import com.badlogic.gdx.Input.Keys.F9
import com.badlogic.gdx.Input.Keys.LEFT
import com.badlogic.gdx.Input.Keys.NUMPAD_1
import com.badlogic.gdx.Input.Keys.NUMPAD_2
import com.badlogic.gdx.Input.Keys.NUM_1
import com.badlogic.gdx.Input.Keys.NUM_2
import com.badlogic.gdx.Input.Keys.PAGE_DOWN
import com.badlogic.gdx.Input.Keys.PAGE_UP
import com.badlogic.gdx.Input.Keys.Q
import com.badlogic.gdx.Input.Keys.RIGHT
import com.badlogic.gdx.Input.Keys.S
import com.badlogic.gdx.Input.Keys.SHIFT_LEFT
import com.badlogic.gdx.Input.Keys.SHIFT_RIGHT
import com.badlogic.gdx.Input.Keys.UP
import com.badlogic.gdx.Input.Keys.V
import com.badlogic.gdx.Input.Keys.W
import com.badlogic.gdx.InputAdapter
import com.fasterxml.jackson.module.kotlin.readValue
import no.elg.hex.Hex
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import no.elg.hex.hud.MapEditorRenderer
import no.elg.hex.input.editor.OpaquenessEditor
import no.elg.hex.input.editor.OpaquenessEditor.Companion.OPAQUENESS_EDITORS
import no.elg.hex.input.editor.TeamEditor
import no.elg.hex.input.editor.TeamEditor.Companion.TEAM_EDITORS
import no.elg.hex.island.Island
import no.elg.hex.util.findHexagonsWithinRadius
import org.hexworks.mixite.core.api.Hexagon
import kotlin.math.max
import kotlin.math.min

/**
 * @author Elg
 */
object MapEditorInput : InputAdapter() {

  private var quickSavedIsland: String = ""

  const val MAX_BRUSH_SIZE = 10
  const val MIN_BRUSH_SIZE = 1

  var brushRadius: Int = 1
    private set

  var selectedTeam: Team = Team.values().first()
    private set

  var saveSlot: Int = 0
    private set

  var opaquenessEditor: OpaquenessEditor = OpaquenessEditor.`Set transparent`
    private set
  var teamEditor: TeamEditor = TeamEditor.Disabled
    private set

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (button == Buttons.LEFT) {
      val cursorHex = BasicInputHandler.cursorHex ?: return true

      fun editHex(hexagon: Hexagon<HexagonData>) {
        opaquenessEditor.edit(hexagon)
        teamEditor.edit(hexagon)
      }

      if (isShiftPressed()) {
        for (hexagon in cursorHex.findHexagonsWithinRadius(brushRadius)) {
          editHex(hexagon)
        }
      } else {
        editHex(cursorHex)
      }
      return true
    }
    return false
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      F1 -> MapEditorRenderer.showHelp = !MapEditorRenderer.showHelp

      W, PAGE_UP, UP -> brushRadius = min(brushRadius + 1, MAX_BRUSH_SIZE)
      S, PAGE_DOWN, DOWN -> brushRadius = max(brushRadius - 1, MIN_BRUSH_SIZE)
      Q -> selectedTeam = Team.values().next(selectedTeam)

      NUM_1, NUMPAD_1 -> opaquenessEditor = OPAQUENESS_EDITORS.next(opaquenessEditor)
      NUM_2, NUMPAD_2 -> teamEditor = TEAM_EDITORS.next(teamEditor)

      F5 -> quickSavedIsland = writeIslandAsString(true)
      F9 -> Hex.island = Hex.mapper.readValue(quickSavedIsland)

      RIGHT -> saveSlot++
      LEFT -> saveSlot = max(saveSlot - 1, 0)

      C -> if (isControlPressed()) Hex.island.saveIsland() else return false
      V -> if (isControlPressed()) Island.loadIsland() else return false
      else -> return false
    }
    return true
  }

  private fun writeIslandAsString(pretty: Boolean = false): String {
    return Hex.mapper.let {
      if (pretty) it.writerWithDefaultPrettyPrinter() else it.writer()
    }.writeValueAsString(Hex.island)
  }


  fun quicksave() {
    quickSavedIsland = Hex.island.serialize()
  }

  fun quickload() {
    Hex.island = Island.deserialize(quickSavedIsland)
  }


  private fun isShiftPressed(): Boolean = Gdx.input.isKeyPressed(SHIFT_LEFT) || Gdx.input.isKeyPressed(SHIFT_RIGHT)
  private fun isControlPressed(): Boolean = Gdx.input.isKeyPressed(CONTROL_LEFT) || Gdx.input.isKeyPressed(CONTROL_RIGHT)

  private fun <E> List<E>.next(current: E): E {
    val nextIndex = (this.indexOf(current) + 1) % this.size
    return this[nextIndex]
  }

  private fun <E> Array<E>.next(current: E): E {
    val nextIndex = (this.indexOf(current) + 1) % this.size
    return this[nextIndex]
  }
}
