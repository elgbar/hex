package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import ktx.actors.isShown
import ktx.actors.minusAssign
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.StageWidget
import ktx.scene2d.actors
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.menu
import ktx.scene2d.vis.menuBar
import ktx.scene2d.vis.menuItem
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visWindow
import no.elg.hex.Hex
import no.elg.hex.hexagon.PIECES
import no.elg.hex.hexagon.PIECES_ORGANIZED
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Team
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.MapEditorRenderer
import no.elg.hex.hud.MessagesRenderer.publishError
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.input.MapEditorInputProcessor
import no.elg.hex.input.editor.Editor
import no.elg.hex.input.editor.OpaquenessEditor
import no.elg.hex.input.editor.PieceEditor
import no.elg.hex.input.editor.TeamEditor
import no.elg.hex.input.editor.editorsList
import no.elg.hex.island.Island
import no.elg.hex.island.Island.Companion.MIN_HEX_IN_TERRITORY
import no.elg.hex.island.Island.Companion.UNKNOWN_ROUNDS_TO_BEAT
import no.elg.hex.model.IslandDto
import no.elg.hex.util.confirmWindow
import no.elg.hex.util.fixWrongTreeTypes
import no.elg.hex.util.hide
import no.elg.hex.util.next
import no.elg.hex.util.onInteract
import no.elg.hex.util.play
import no.elg.hex.util.previous
import no.elg.hex.util.regenerateCapitals
import no.elg.hex.util.removeSmallerIslands
import no.elg.hex.util.saveInitialIsland
import no.elg.hex.util.separator
import no.elg.hex.util.show
import no.elg.hex.util.toTitleCase
import no.elg.hex.util.toggleShown
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/** @author Elg */
class MapEditorScreen(id: Int, island: Island) : PreviewIslandScreen(id, island, isPreviewRenderer = false) {

  private val stageScreen = StageScreen()
  private val mapInputProcessor = MapEditorInputProcessor(this)
  private val frameUpdatable = MapEditorRenderer(this)
  private val debugInfoRenderer = DebugInfoRenderer(this)
  private lateinit var quickSavedIsland: IslandDto

  private val editorsWindows: MutableMap<KVisWindow, KVisWindow.() -> Unit> = mutableMapOf()

  private val confirmExit: KVisWindow

  var brushRadius: Int = 1
    private set(value) {
      field = value.coerceIn(MIN_BRUSH_SIZE, MAX_BRUSH_SIZE)
    }

  var selectedTeam: Team = Team.entries.first()
    private set

  var selectedPiece: KClass<out Piece> = PIECES.first()
    private set

  var editor: Editor = OpaquenessEditor.ToggleOpaqueness
  val artbSpinner = IntSpinnerModel(island.authorRoundsToBeat, 0, Int.MAX_VALUE)

  init {
    quicksave()
    stageScreen.stage.actors {

      confirmExit = confirmWindow(
        "Confirm exit",
        "Do you want to save before exiting?",
        whenDenied = { Hex.screen = LevelSelectScreen() },
        whenConfirmed = {
          if (saveInitialIsland(id, island)) {
            Hex.screen = LevelSelectScreen()
          } else {
            this@confirmWindow.fadeOut()
          }
        }
      )

      visWindow("Enter Author Round to Beat") {
        visLabel("Lowest number of rounds\nto beat this level")
        row()
        spinner("", artbSpinner) {
          textField.addValidator { input ->
            val intInput = input?.toIntOrNull() ?: -1
            intInput >= UNKNOWN_ROUNDS_TO_BEAT
          }

          onChange {
            if (textField.isInputValid) {
              island.authorRoundsToBeat = textField.text?.toIntOrNull() ?: UNKNOWN_ROUNDS_TO_BEAT
            }
          }
        }
        pack()
      }.also {
        editorsWindows[it] = {
          centerWindow()
          setPosition(0f, y * 3f / 2f)
        }
      }

      fun exit() {
        confirmExit.centerWindow()
        confirmExit.toggleShown(stage)
      }

      @Scene2dDsl
      fun <T> StageWidget.itemsWindow(
        title: String,
        items: Iterable<Iterable<T>>,
        stringifyItem: (T) -> String,
        onResize: KVisWindow.() -> Unit,
        onButtonClick: (T) -> Unit
      ): KVisWindow =
        visWindow(title) {
          isResizable = false
          titleLabel.setAlignment(Align.center)
          defaults().space(5f).padLeft(2.5f).padRight(2.5f).padBottom(2.5f)
          var lastChecked: VisTextButton? = null
          for (itemRow in items) {
            for (item in itemRow) {
              visTextButton(stringifyItem(item).toTitleCase(), style = "mapeditor-editor-item") {
                pad(5f)
                onClick {
                  lastChecked?.isDisabled = false
                  this.isDisabled = true
                  lastChecked = this

                  onButtonClick(item)
                }.also { clickListener ->
                  if (lastChecked == null) {
                    clickListener.clicked(InputEvent(), 0f, 0f)
                  }
                }
                it.fillX()
              }
            }
            row()
          }

          pack()
        }.also {
          editorsWindows[it] = {
            centerWindow()
            onResize()
          }
        }
      itemsWindow(
        title = "Team",
        items = listOf(Team.entries),
        stringifyItem = { it.name.lowercase() },
        onResize = { setPosition(0f, y) },
        onButtonClick = { this@MapEditorScreen.selectedTeam = it }
      )

      itemsWindow(
        title = "Piece",
        items = PIECES_ORGANIZED,
        stringifyItem = { it.simpleName ?: it.jvmName },
        onResize = { setPosition(0f, y / 2) },
        onButtonClick = { this@MapEditorScreen.selectedPiece = it }
      )

      itemsWindow(
        title = "Editors",
        items = editorsList,
        stringifyItem = { it::class.simpleName ?: it::class.jvmName },
        onResize = { setPosition(parent.width, 0f) },
        onButtonClick = { this@MapEditorScreen.editor = it }
      )

      val infoWindow = visWindow("Island editor information") {
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
            |  (i.e., there can only be one island)
            |* No capital pieces in territories with size smaller than $MIN_HEX_IN_TERRITORY
            |* There must be exactly one capital per territory
            |* Pine tree can not have an invisible hexagons next to them
            |* Palms trees can not only be surrendered by visible hexagons
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
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.CONTROL_LEFT, Keys.S) {
                saveInitialIsland(id, island)
              }
            }
            menuItem("Reload") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.CONTROL_LEFT, Keys.R) {

                if (play(id)) {
                  publishMessage("Successfully reloaded island $id")
                } else {
                  publishError("Failed to reload island $id")
                }
              }
            }
            separator()
            menuItem("Quick save") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.F5) { quicksave() }
            }
            menuItem("Quick Load") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.F9) { quickload() }
            }
            separator()
            menuItem("Exit") {
              onInteract(
                this@MapEditorScreen.stageScreen.stage,
                Keys.ESCAPE,
                catchEvent = true
              ) { exit() }
            }
          }

          menu("Edit") {
            menuItem("Regenerate Capitals") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.CONTROL_LEFT, Keys.C) {
                island.regenerateCapitals()
              }
            }
            menuItem("Remove Smaller Islands") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.CONTROL_LEFT, Keys.K) {
                island.removeSmallerIslands()
              }
            }

            menuItem("Fix wrong tree types") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.CONTROL_LEFT, Keys.T) {
                island.fixWrongTreeTypes()
              }
            }

            separator()

            menuItem("Increase Brush Size") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.UP) {
                brushRadius++
              }
            }
            menuItem("Decrease Brush Size") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.DOWN) {
                brushRadius--
              }
            }

            separator()

            menuItem("Editor Type Specific") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.Q) {
                if (editor is TeamEditor) {
                  selectedTeam = Team.entries.toTypedArray().let {
                    if (shiftPressed) it.previous(selectedTeam) else it.next(selectedTeam)
                  }
                } else if (editor is PieceEditor) {
                  selectedPiece = PIECES.let {
                    if (shiftPressed) it.previous(selectedPiece) else it.next(selectedPiece)
                  }
                }
              }
            }
          }
          menu("Tools") {
            menuItem("Toggle Editor Types") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.F1) {
                editorsWindows.forEach { it.key.toggleShown(this@MapEditorScreen.stageScreen.stage) }
              }
            }
            menuItem("Set Author Round to Beat (ARtB)") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.F2) {
              }
            }
          }

          menu("Help") {
            menuItem("Information") { onClick { infoWindow.show(this@MapEditorScreen.stageScreen.stage) } }
          }
        }
        setFillParent(true)
      }
    }
  }

  private fun quicksave() {
    quickSavedIsland = island.createDto()
  }

  private fun quickload() {
    if (!::quickSavedIsland.isInitialized) {
      publishError("No quick save found")
      return
    }
    island.restoreState(quickSavedIsland)
  }

  override fun render(delta: Float) {
    super.render(delta)
    frameUpdatable.frameUpdate()
    debugInfoRenderer.frameUpdate()
    stageScreen.render(delta)
  }

  override fun show() {
    stageScreen.show()
    mapInputProcessor.show()
    super.show()
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)

    val filteredEditorsWindows = editorsWindows.filter {
      val window = it.key
      window.isShown().also { b -> if (b) stageScreen.stage -= window }
    }
    stageScreen.resize(width, height)
    filteredEditorsWindows.forEach {
      it.key.show(stageScreen.stage, false, 0f)
      it.value(it.key)
    }
  }

  override fun dispose() {
    super.dispose()
    stageScreen.dispose()
    debugInfoRenderer.dispose()
  }

  companion object {
    const val MAX_BRUSH_SIZE = 10
    const val MIN_BRUSH_SIZE = 1
    private val shiftPressed get() = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT)
  }
}