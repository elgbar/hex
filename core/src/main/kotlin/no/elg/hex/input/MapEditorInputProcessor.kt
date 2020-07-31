package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys.A
import com.badlogic.gdx.Input.Keys.C
import com.badlogic.gdx.Input.Keys.CONTROL_LEFT
import com.badlogic.gdx.Input.Keys.CONTROL_RIGHT
import com.badlogic.gdx.Input.Keys.DOWN
import com.badlogic.gdx.Input.Keys.F1
import com.badlogic.gdx.Input.Keys.F5
import com.badlogic.gdx.Input.Keys.F9
import com.badlogic.gdx.Input.Keys.NUMPAD_1
import com.badlogic.gdx.Input.Keys.NUMPAD_2
import com.badlogic.gdx.Input.Keys.NUMPAD_3
import com.badlogic.gdx.Input.Keys.NUM_1
import com.badlogic.gdx.Input.Keys.NUM_2
import com.badlogic.gdx.Input.Keys.NUM_3
import com.badlogic.gdx.Input.Keys.PAGE_DOWN
import com.badlogic.gdx.Input.Keys.PAGE_UP
import com.badlogic.gdx.Input.Keys.Q
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
import no.elg.hex.hexagon.PIECES
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Team
import no.elg.hex.input.editor.OpaquenessEditor
import no.elg.hex.input.editor.PieceEditor
import no.elg.hex.input.editor.TeamEditor
import no.elg.hex.screens.IslandScreen
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.util.findHexagonsWithinRadius
import no.elg.island.Island
import org.hexworks.mixite.core.api.Hexagon
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * @author Elg
 */
class MapEditorInputProcessor(
  private val islandScreen: IslandScreen
) : InputAdapter() {


  private val opaquenessEditors = OpaquenessEditor.generateOpaquenessEditors(islandScreen)
  private val teamEditors = TeamEditor.generateTeamEditors(islandScreen)
  private val pieceEditors = PieceEditor.generatePieceEditors(islandScreen)

  init {
    quicksave()
  }

  private var quickSavedIsland: String = ""

  var showHelp = false

  var brushRadius: Int = 1
    private set

  var selectedTeam: Team = Team.values().first()
    private set

  var selectedPiece: KClass<out Piece> = PIECES.first()
    private set

  var opaquenessEditor: OpaquenessEditor = opaquenessEditors.last()
    private set
  var teamEditor: TeamEditor = teamEditors.last()
    private set
  var pieceEditor: PieceEditor = pieceEditors.last()
    private set


  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (button == Buttons.LEFT) {
      val cursorHex = islandScreen.basicInputProcessor.cursorHex ?: return true

      fun editHex(hexagon: Hexagon<HexagonData>) {
        opaquenessEditor.edit(hexagon)
        teamEditor.edit(hexagon)
        pieceEditor.edit(hexagon)
      }

      if (isShiftPressed()) {
        for (hexagon in cursorHex.findHexagonsWithinRadius(islandScreen.island, brushRadius)) {
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
      F1 -> showHelp = !showHelp

      W, PAGE_UP, UP -> brushRadius = min(brushRadius + 1, MAX_BRUSH_SIZE)
      S, PAGE_DOWN, DOWN -> brushRadius = max(brushRadius - 1, MIN_BRUSH_SIZE)

      Q -> selectedTeam = Team.values().let { if (isShiftPressed()) it.previous(selectedTeam) else it.next(selectedTeam) }
      A -> selectedPiece = PIECES.let { if (isShiftPressed()) it.previous(selectedPiece) else it.next(selectedPiece) }

      NUM_1, NUMPAD_1 ->
        opaquenessEditor = opaquenessEditors.let { if (isShiftPressed()) it.previous(opaquenessEditor) else it.next(opaquenessEditor) }
      NUM_2, NUMPAD_2 ->
        teamEditor = teamEditors.let { if (isShiftPressed()) it.previous(teamEditor) else it.next(teamEditor) }
      NUM_3, NUMPAD_3 ->
        pieceEditor = pieceEditors.let { if (isShiftPressed()) it.previous(pieceEditor) else it.next(pieceEditor) }

      F5 -> quicksave()
      F9 -> quickload()

      C -> if (isControlPressed()) islandScreen.saveIsland() else return false
      V -> if (isControlPressed()) {
        LevelSelectScreen.play(islandScreen.basicInputProcessor.saveSlot)
      } else return false
      else -> return false
    }
    return true
  }

  fun quicksave() {
    quickSavedIsland = islandScreen.island.serialize()
  }

  fun quickload() {
    LevelSelectScreen.play(Hex.mapper.readValue<Island>(quickSavedIsland))
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

  private fun <E> List<E>.previous(current: E): E {
    val currentIndex = this.indexOf(current)
    val nextIndex = if (currentIndex == 0) size - 1 else (currentIndex - 1) % this.size
    return this[nextIndex]
  }

  private fun <E> Array<E>.previous(current: E): E {
    val currentIndex = this.indexOf(current)
    val nextIndex = if (currentIndex == 0) size - 1 else (currentIndex - 1) % this.size
    return this[nextIndex]
  }

  companion object {
    const val MAX_BRUSH_SIZE = 10
    const val MIN_BRUSH_SIZE = 1
  }
}
