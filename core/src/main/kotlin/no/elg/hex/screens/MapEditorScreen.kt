package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.Spinner
import ktx.actors.isShown
import ktx.actors.minusAssign
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.onClickEvent
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.StageWidget
import ktx.scene2d.actors
import ktx.scene2d.vis.KVisTextButton
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.menu
import ktx.scene2d.vis.menuBar
import ktx.scene2d.vis.menuItem
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visCheckBox
import ktx.scene2d.vis.visImage
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import ktx.scene2d.vis.visWindow
import no.elg.hex.Hex
import no.elg.hex.hexagon.PIECES
import no.elg.hex.hexagon.PIECES_ORGANIZED
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Team
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.MapEditorRenderer
import no.elg.hex.hud.MessagesRenderer
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
import no.elg.hex.island.Island.Companion.NEVER_BEATEN
import no.elg.hex.island.Island.Companion.NEVER_PLAYED
import no.elg.hex.island.Island.Companion.SPECIAL_MAP
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.model.IslandDto
import no.elg.hex.util.cleanPiecesOnInvisibleHexagons
import no.elg.hex.util.confirmWindow
import no.elg.hex.util.createHandInstance
import no.elg.hex.util.fixWrongTreeTypes
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.hide
import no.elg.hex.util.onInteract
import no.elg.hex.util.play
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
class MapEditorScreen(metadata: FastIslandMetadata, island: Island) : PreviewIslandScreen(metadata, island, isPreviewRenderer = false) {

  private val stageScreen = StageScreen()
  private val mapInputProcessor = MapEditorInputProcessor(this)
  private val frameUpdatable = MapEditorRenderer(this)
  private val debugInfoRenderer = DebugInfoRenderer(this)

  private val editorsWindows: MutableMap<KVisWindow, KVisWindow.() -> Unit> = mutableMapOf()
  private val editorsButtons: MutableList<(Any) -> Unit> = mutableListOf()

  private val confirmExit: KVisWindow

  private val initialIsland = island.createDto()
  private var quickSavedIsland: IslandDto = initialIsland

  var brushRadius: Int = 1
    private set(value) {
      field = value.coerceIn(MIN_BRUSH_SIZE, MAX_BRUSH_SIZE)
    }

  var selectedTeam: Team = Team.entries.first()
    private set

  var selectedPiece: KClass<out Piece> = PIECES.first()
    private set

  var editor: Editor = OpaquenessEditor.ToggleOpaqueness
  val artbSpinner = IntSpinnerModel(metadata.authorRoundsToBeat, Int.MIN_VALUE, Int.MAX_VALUE)

  init {
    stageScreen.stage.actors {

      confirmExit = confirmWindow(
        "Confirm exit",
        "Do you want to save before exiting?",
        whenDenied = { Hex.screen = LevelSelectScreen() },
        whenConfirmed = {
          if (saveInitialIsland(metadata, island)) {
            Hex.screen = LevelSelectScreen()
          } else {
            this@confirmWindow.fadeOut()
          }
        }
      )

      visWindow("Additional options") {
        visTable {

          visTable {
            visTextTooltip(
              """
              Test maps are not shown in the level 
              select screen unless either map editor
              mode or debug mode is enabled.
              """.trimIndent()
            )
            visLabel("Is test map? ")
            visCheckBox("") {
              isChecked = metadata.forTesting
              onChange {
                metadata.forTesting = isChecked
              }
            }
          }
          row()
          separator()
          row()

          visTable {
            visTextTooltip(
              """
              Author Round to Beat (ARtB) is the 
              lowest number of rounds to beat
              this level and used to calculate 
              the difficulty  of the level.
              """.trimIndent()
            )

            visLabel(
              """
              Previous ARtB: ${metadata.authorRoundsToBeat}
              $NEVER_PLAYED = not played
              ${NEVER_PLAYED - 1} = always last
              $NEVER_BEATEN = never beaten (but played)
              """.trimIndent()
            )
            row()
            spinner("ARtB", artbSpinner) {
              textFieldEventPolicy = Spinner.TextFieldEventPolicy.ON_KEY_TYPED
              onChange {
                val parsedInt = textField.text?.toIntOrNull() ?: NEVER_PLAYED
                metadata.authorRoundsToBeat = if (parsedInt == NEVER_PLAYED - 1) SPECIAL_MAP else parsedInt
              }
              it.prefWidth(Value.percentWidth(0.5f, this@visWindow))
              it.prefWidth(Value.percentHeight(0.5f, this@visWindow))
            }
            pack()
          }
        }
        keepWithinStage()
        pack()
      }.also {
        editorsWindows[it] = {
          setPosition(Gdx.graphics.width.toFloat() - MIN_DIST_FROM_EDGE - width, Gdx.graphics.height.toFloat() / 2f - height / 2f)
        }
      }

      fun exit() {
        if (!getIslandFile(metadata.id).exists() || initialIsland != island.createDto()) {
          confirmExit.centerWindow()
          confirmExit.toggleShown(stage)
        } else {
          Hex.screen = LevelSelectScreen()
        }
      }

      @Scene2dDsl
      fun <T : Any> StageWidget.itemsWindow(
        title: String,
        items: Iterable<Iterable<T>>,
        stringifyItem: (T) -> String,
        onResize: KVisWindow.() -> Unit,
        onButtonClick: (T) -> Unit,
        onOtherClicked: (item: T, thisButton: KVisTextButton) -> Unit = { _, _ -> },
        icon: (T) -> Pair<TextureRegion, Color?>? = { null }
      ): KVisWindow =
        visWindow(title) {
          isResizable = false
          style.stageBackground = null
          titleLabel.setAlignment(Align.center)
          defaults().space(5f).padLeft(2.5f).padRight(2.5f).padBottom(2.5f)
          var lastChecked: VisTextButton? = null
          for (itemRow in items) {
            for (item in itemRow) {
              icon(item)?.also { (icon, maybeColor) ->
                val size = 15f
                val flippedIcon = AtlasRegion(icon).also { it.flip(false, true) }
                visImage(flippedIcon) {
                  maybeColor?.also { newColor ->
                    this.color = newColor
                  }
                  it.minSize(size)
                  it.maxSize(size)
                  it.fillX()
                }
              }
              visTextButton(stringifyItem(item).toTitleCase(), style = "mapeditor-editor-item") {
                pad(5f)
                editorsButtons += { onOtherClicked(item, this) }
                onClickEvent { _ ->
                  lastChecked?.isDisabled = false
                  this.isDisabled = true
                  lastChecked = this

                  onButtonClick(item)
                  for (function in editorsButtons) {
                    function(item)
                  }
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
        onResize = { setPosition(MIN_DIST_FROM_EDGE, y) },
        onButtonClick = {
          this@MapEditorScreen.selectedTeam = it
          this@MapEditorScreen.editor = TeamEditor.SetTeam
        },
        icon = { Hex.assets.surrender to it.color }
      )

      itemsWindow(
        title = "Piece",
        items = PIECES_ORGANIZED,
        stringifyItem = { it.simpleName ?: it.jvmName },
        onResize = { setPosition(MIN_DIST_FROM_EDGE, y / 2) },
        onButtonClick = {
          this@MapEditorScreen.selectedPiece = it
          this@MapEditorScreen.editor = PieceEditor.SetPiece
        },
        icon = { (Hex.assets.getTexture(it.createHandInstance(), false) ?: Hex.assets.hand) to null }
      )

      itemsWindow(
        title = "Editors",
        items = editorsList,
        stringifyItem = { it::class.simpleName ?: it::class.jvmName },
        onResize = { setPosition(MIN_DIST_FROM_EDGE, MIN_DIST_FROM_EDGE) },
        onButtonClick = { this@MapEditorScreen.editor = it },
        onOtherClicked = { item, button ->
          button.isDisabled = item == this@MapEditorScreen.editor
        }
      )

      val infoWindow = visWindow("Island validation information") {
        closeOnEscape()
        addCloseButton()
        isModal = true
        visLabel(
          """
            |Island Validation Rules:
            |
            |* All visible hexagons must be reachable from all other visible hexagons 
            |  (i.e., there can only be one island)
            |* No capital pieces in territories with size smaller than $MIN_HEX_IN_TERRITORY
            |* There must be exactly one capital per territory
            |* No pieces on invisible hexagons
            |* Each team must have at least one capital
            |
            |Trees specific rules:
            |* Pine tree can not have an invisible hexagons next to them
            |* Palms trees can not only be surrendered by visible hexagons
            |* The lastGrown property of trees must be equal to the hex's current team ordinal
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
                saveInitialIsland(metadata, island)
              }
            }
            menuItem("Reload") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.CONTROL_LEFT, Keys.R) {

                if (play(metadata)) {
                  publishMessage("Successfully reloaded island ${metadata.id}")
                } else {
                  publishError("Failed to reload island ${metadata.id}")
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
                resetARtBOnEdit()
              }
            }
            menuItem("Remove Smaller Islands") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.CONTROL_LEFT, Keys.K) {
                island.removeSmallerIslands()
                resetARtBOnEdit()
              }
            }

            menuItem("Fix wrong tree types") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.CONTROL_LEFT, Keys.T) {
                island.fixWrongTreeTypes()
                resetARtBOnEdit()
              }
            }
            menuItem("Remove pieces on invisible hexagons") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.CONTROL_LEFT, Keys.I) {
                island.cleanPiecesOnInvisibleHexagons()
                resetARtBOnEdit()
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
          }
          menu("Tools") {
            menuItem("Toggle Editor Types") {
              onInteract(this@MapEditorScreen.stageScreen.stage, Keys.F1) {
                editorsWindows.forEach { it.key.toggleShown(this@MapEditorScreen.stageScreen.stage) }
              }
            }
          }

          menu("Help") {
            menuItem("Validation Rules") { onClick { infoWindow.show(this@MapEditorScreen.stageScreen.stage) } }
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
    island.restoreState(quickSavedIsland)
  }

  fun resetARtBOnEdit() {
    if (artbSpinner.value != NEVER_PLAYED) {
      MessagesRenderer.publishWarning("Resetting rounds to beat to unknown as the map has been edited")
      artbSpinner.setValue(NEVER_PLAYED, true)
    }
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

    const val MIN_DIST_FROM_EDGE = 5f
  }
}