package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.fasterxml.jackson.module.kotlin.readValue
import com.kotcrab.vis.ui.widget.ButtonBar
import ktx.actors.onClick
import ktx.scene2d.actors
import ktx.scene2d.scene2d
import ktx.scene2d.vis.buttonBar
import ktx.scene2d.vis.menu
import ktx.scene2d.vis.menuBar
import ktx.scene2d.vis.menuItem
import ktx.scene2d.vis.visImageTextButton
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import ktx.scene2d.vis.visWindow
import no.elg.hex.Hex
import no.elg.hex.hexagon.PIECES
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Team
import no.elg.hex.hud.MapEditorRenderer
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.input.MapEditorInputProcessor
import no.elg.hex.input.editor.Editor
import no.elg.hex.input.editor.NOOPEditor
import no.elg.hex.input.editor.OpaquenessEditor
import no.elg.hex.input.editor.PieceEditor
import no.elg.hex.input.editor.TeamEditor
import no.elg.hex.island.Island
import no.elg.hex.island.Island.Companion.MIN_HEX_IN_TERRITORY
import no.elg.hex.util.hide
import no.elg.hex.util.next
import no.elg.hex.util.nextOrNull
import no.elg.hex.util.onInteract
import no.elg.hex.util.play
import no.elg.hex.util.previous
import no.elg.hex.util.previousOrNull
import no.elg.hex.util.regenerateCapitals
import no.elg.hex.util.saveIsland
import no.elg.hex.util.separator
import no.elg.hex.util.serialize
import no.elg.hex.util.show
import no.elg.hex.util.toggleShown
import kotlin.reflect.KClass

/** @author Elg */
class MapEditorScreen(val id: Int, val island: Island) : StageScreen() {

  private val islandScreen = PlayableIslandScreen(id, island)
  val basicIslandInputProcessor
    get() = islandScreen.basicIslandInputProcessor

  val inputProcessor = MapEditorInputProcessor(this)
  private val frameUpdatable = MapEditorRenderer(this)
  private var quickSavedIsland: String = ""

  private val opaquenessEditors = OpaquenessEditor.generateOpaquenessEditors(this)
  private val teamEditors = TeamEditor.generateTeamEditors(this)
  private val pieceEditors = PieceEditor.generatePieceEditors(this)

  var brushRadius: Int = 1
    internal set(value) {
      field = value.coerceIn(MIN_BRUSH_SIZE, MAX_BRUSH_SIZE)
    }

  var selectedTeam: Team = Team.values().first()
    internal set

  var selectedPiece: KClass<out Piece> = PIECES.first()
    internal set

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
        MessagesRenderer.publishError("Wrong editor type given: $value. Expected one of $editors or $NOOPEditor")
      }
    }

  init {
    quicksave()
    stage.actors {
      val confirmExit =
        visWindow("Confirm exit") {
          isMovable = false
          isModal = true
          hide()

          visLabel("Do you want to save before exiting?")
          row()

          buttonBar {
            setButton(
              ButtonBar.ButtonType.YES,
              scene2d.visTextButton("Yes") {
                onClick {
                  if (saveIsland(id, island)) {
                    Hex.screen = LevelSelectScreen
                  } else {
                    this@visWindow.fadeOut()
                  }
                }
              }
            )

            setButton(
              ButtonBar.ButtonType.NO,
              scene2d.visTextButton("No") { onClick { Hex.screen = LevelSelectScreen } }
            )

            setButton(
              ButtonBar.ButtonType.CANCEL,
              scene2d.visTextButton("Cancel") { onClick { this@visWindow.fadeOut() } }
            )
            createTable().pack()
          }
          pack()
          centerWindow()
        }

      fun exit() {
        confirmExit.toggleShown(this@MapEditorScreen.stage)
      }

      val editorsWindow =
        visWindow("Editors") {
          addCloseButton()
          isResizable = false

          if (Hex.debug) {
            debug()
          }
          defaults().space(5f)

          fun createEditorButton(name: String, key: Int) {
            visImageTextButton(name) {
              onClick { Hex.inputMultiplexer.keyDown(key) }
              it.fillX()
              visTextTooltip("Hotkey: ${Keys.toString(key)}")
            }
          }
          createEditorButton("Opaqueness", OPAQUENESS_KEY)
          row()
          createEditorButton("Team", TEAM_KEY)
          row()
          createEditorButton("Piece", PIECE_KEY)
          pack()
        }

      val infoWindow =
        visWindow("Island editor information") {
          closeOnEscape()
          addCloseButton()
          isModal = true
          visLabel(
            """
              |Tips and Tricks:
              |* Holding SHIFT will reverse iteration order, unless otherwise stated
              |
              |Island Validation rules:
              |
              |* All visible hexagons must be reachable from all other visible hexagons 
              |  (ie there can only be one island)
              |* No capital pieces in territories with size smaller than $MIN_HEX_IN_TERRITORY
              |* There must be exactly one capital per territory
            """.trimMargin()
          ) {
            it.expand().fill()
          }
          pack()
          centerWindow()
          this.hide()
        }

      visTable {
        menuBar { cell ->
          cell.top().growX().expandY()

          menu("Island") {
            menuItem("Save") {
              onInteract(this@MapEditorScreen.stage, Keys.CONTROL_LEFT, Keys.S) {
                saveIsland(id, island)
              }
            }
            menuItem("Reload") {
              onInteract(this@MapEditorScreen.stage, Keys.CONTROL_LEFT, Keys.R) {
                play(id)
                publishMessage("Successfully reloaded island $id")
              }
            }
            separator()
            menuItem("Quick save") {
              onInteract(this@MapEditorScreen.stage, Keys.F5) { quicksave() }
            }
            menuItem("Quick Load") {
              onInteract(this@MapEditorScreen.stage, Keys.F9) { quickload() }
            }
            separator()
            menuItem("Exit") { onInteract(this@MapEditorScreen.stage, Keys.ESCAPE) { exit() } }
          }

          menu("Edit") {
            menuItem("Regenerate Capitals") {
              onInteract(this@MapEditorScreen.stage, Keys.CONTROL_LEFT, Keys.C) {
                island.regenerateCapitals()
              }
            }

            separator()

            menuItem("Next Editor") {
              onInteract(this@MapEditorScreen.stage, Keys.RIGHT) {
                editor =
                  editors.nextOrNull(editor) ?: NOOPEditor
              }
            }

            menuItem("Previous Editor") {
              onInteract(this@MapEditorScreen.stage, Keys.LEFT) {
                editor =
                  editors.previousOrNull(editor) ?: NOOPEditor
              }
            }

            separator()

            menuItem("Increase Brush Size") {
              onInteract(this@MapEditorScreen.stage, Keys.PAGE_UP) {
                brushRadius++
              }
            }
            menuItem("Decrease Brush Size") {
              onInteract(this@MapEditorScreen.stage, Keys.PAGE_DOWN) {
                brushRadius--
              }
            }

            separator()

            menuItem("Editor Type Specific") {
              onInteract(this@MapEditorScreen.stage, Keys.Q) {
                if (editor is TeamEditor) {
                  selectedTeam =
                    Team.values().let {
                      if (shiftPressed) it.previous(selectedTeam) else it.next(selectedTeam)
                    }
                } else if (editor is PieceEditor) {
                  selectedPiece =
                    PIECES.let {
                      if (shiftPressed) it.previous(selectedPiece) else it.next(selectedPiece)
                    }
                }
              }
            }
          }
          menu("Tools") {
            menuItem("Toggle Editor Types") {
              onInteract(this@MapEditorScreen.stage, Keys.F1) {
                editorsWindow.toggleShown(this@MapEditorScreen.stage)
              }
            }

            separator()

            menuItem("Opaqueness Editor Type") {
              onInteract(this@MapEditorScreen.stage, OPAQUENESS_KEY) {
                editors = opaquenessEditors
              }
            }
            menuItem("Team Editor Type") {
              onInteract(this@MapEditorScreen.stage, TEAM_KEY) {
                editors = teamEditors
              }
            }
            menuItem("Piece Editor Type") {
              onInteract(this@MapEditorScreen.stage, PIECE_KEY) {
                editors = pieceEditors
              }
            }
          }

          menu("Help") {
            menuItem("Information") { onClick { infoWindow.show(this@MapEditorScreen.stage) } }
          }
        }
        setFillParent(true)
      }
    }
  }

  fun quicksave() {
    quickSavedIsland = island.serialize()
  }

  fun quickload() {
    if (quickSavedIsland.isEmpty()) {
      MessagesRenderer.publishError("No quick save found")
      return
    }
    play(id, Hex.mapper.readValue(quickSavedIsland))
  }

  override fun render(delta: Float) {
    islandScreen.render(delta)
    frameUpdatable.frameUpdate()
    super.render(delta)
  }

  override fun show() {
    super.show()
    Hex.inputMultiplexer.addProcessor(stage)
    Hex.inputMultiplexer.addProcessor(basicIslandInputProcessor)
    Hex.inputMultiplexer.addProcessor(inputProcessor)
  }

  override fun hide() {
    super.hide()
    Hex.inputMultiplexer.removeProcessor(stage)
    Hex.inputMultiplexer.removeProcessor(basicIslandInputProcessor)
    Hex.inputMultiplexer.removeProcessor(inputProcessor)
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    islandScreen.resize(width, height)
  }

  companion object {
    const val MAX_BRUSH_SIZE = 10
    const val MIN_BRUSH_SIZE = 1

    const val OPAQUENESS_KEY = Keys.O
    const val TEAM_KEY = Keys.T
    const val PIECE_KEY = Keys.P

    private val shiftPressed get() = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT)
  }
}
