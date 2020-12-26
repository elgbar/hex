package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.widget.ButtonBar
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.onClick
import ktx.scene2d.KHorizontalGroup
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.scene2d
import ktx.scene2d.table
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.buttonBar
import ktx.scene2d.vis.visImageButton
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import ktx.scene2d.vis.visWindow
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.GameInfoRenderer
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.hud.ScreenText
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.LOST
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.NOTHING
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.SURRENDER
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.WON
import no.elg.hex.util.createHandInstance
import no.elg.hex.util.getData
import no.elg.hex.util.hide
import no.elg.hex.util.onInteract
import no.elg.hex.util.show

/** @author Elg */
@Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
class PlayableIslandScreen(id: Int, island: Island) : PreviewIslandScreen(id, island) {

  val stageScreen = StageScreen()
  val inputProcessor by lazy { GameInputProcessor(this) }
  val gestureDetector by lazy { GestureDetector(inputProcessor) }

  private val frameUpdatable by lazy { GameInfoRenderer(this) }
  private val debugRenderer: DebugInfoRenderer by lazy { DebugInfoRenderer(this) }

  private val confirmEndTurn: VisWindow
  private val confirmSurrender: VisWindow
  internal val acceptAISurrender: VisWindow
  internal val youWon: VisWindow
  internal val youLost: VisWindow

  private val buttonGroup: KHorizontalGroup

  private val disableChecker: MutableMap<Disableable, (Territory?) -> Boolean> = mutableMapOf()
  private val labelUpdater: MutableMap<KVisWindow, KVisWindow.() -> Unit> = mutableMapOf()

  private var modifier = NOTHING

  init {
    stageScreen.stage.actors {

      @Scene2dDsl
      fun confirmWindow(title: String, text: String, whenConfirmed: KVisWindow.() -> Unit): VisWindow {
        return this.visWindow(title) {
          isMovable = false
          isModal = true
          hide()

          visLabel(text)
          row()

          buttonBar {
            setButton(
              ButtonBar.ButtonType.YES,
              scene2d.visTextButton("Yes") {
                onClick {
                  this@visWindow.whenConfirmed()
                  this@visWindow.fadeOut()
                }
              }
            )

            setButton(
              ButtonBar.ButtonType.NO,
              scene2d.visTextButton("No") { onClick { this@visWindow.fadeOut() } }
            )
            createTable().pack()
          }
          pack()
          centerWindow()
          closeOnEscape()
          addCloseButton()
          fadeOut(0f)
        }
      }

      @Scene2dDsl
      fun okWindow(title: String, text: () -> String, whenConfirmed: KVisWindow.() -> Unit): VisWindow {
        return visWindow(title) {
          isMovable = false
          isModal = true
          this.hide()
          val label = visLabel(text())

          labelUpdater[this] = {
            label.setText(text())
            pack()
            centerWindow()
          }

          row()

          fadeIn()

          buttonBar {
            setButton(
              ButtonBar.ButtonType.OK,
              scene2d.visTextButton("OK") {
                onClick {
                  this@visWindow.whenConfirmed()
                  this@visWindow.fadeOut()
                }
              }
            )
            createTable().pack()
          }
          pack()
          centerWindow()
          fadeOut(0f)
        }
      }

      fun endGame(modifier: PreviewModifier): KVisWindow.() -> Unit = {
        island.history.disable()
        island.history.clear()
        this@PlayableIslandScreen.modifier = modifier
        Hex.screen = LevelSelectScreen
        island.restoreInitialState()
      }

      youWon = okWindow("You Won!", { "Congratulations! You won in ${island.turn} turns." }, endGame(WON))
      youLost = okWindow("You Lost", { "Too bad! You lost in ${island.turn} turns." }, endGame(LOST))

      confirmEndTurn = confirmWindow("Confirm End Turn", "Are you sure you want to end your turn?") {
        island.endTurn(inputProcessor)
      }

      confirmSurrender = confirmWindow("Confirm Surrender", "Are you sure you want to surrender?") {
        surrender()
      }

      acceptAISurrender = confirmWindow("Accept AI Surrender", "The AI want to surrender, do you accept?") {
        island.history.disable()
        island.history.clear()

        for (hexagon in island.hexagons) {
          val data = island.getData(hexagon)
          if (data.team == island.currentTeam) {
            continue
          }
          data.team = island.currentTeam
          data.setPiece(Empty::class)
        }

        island.select(null)
        island.select(
          island.hexagons.first {
            !island.getData(it).invisible
          }
        )
        island.select(null)
        youWon.show(stage)
      }

      table {

        setFillParent(true)
        bottom()
        buttonGroup = horizontalGroup {
          left()
          expand()
          space(20f)
          pad(20f)

          val size = Value.percentWidth(0.1f, this@table)

          @Scene2dDsl
          fun button(
            tooltip: String,
            up: TextureRegion,
            down: TextureRegion? = null,
            disabled: TextureRegion? = null,
            disableCheck: ((Territory?) -> Boolean) = { island.isCurrentTeamAI() },
            vararg keyShortcut: Int,
            onClick: Button.() -> Unit
          ) {

            fun drawableToTextureRegion(drawable: TextureRegion): Drawable {
              val region = TextureRegion(drawable)
              region.flip(false, true)
              return TextureRegionDrawable(region)
            }

            visImageButton {

              visTextTooltip(tooltip)
              style.imageUp = drawableToTextureRegion(up)

              if (down != null) {
                style.imageDown = drawableToTextureRegion(down)
              }
              if (disabled != null) {
                style.disabled = drawableToTextureRegion(disabled)
              }
              onInteract(stage, *keyShortcut, interaction = onClick)
              disableChecker[this] = disableCheck

              background = null
              isTransform = true
              imageCell.size(size)
            }
          }

          button(
            tooltip = "Buy Castle",
            up = Hex.assets.castle,
            disableCheck = { territory -> (territory?.capital?.balance ?: -1) < CASTLE_PRICE && island.isCurrentTeamAI() },
            keyShortcut = intArrayOf(Keys.NUM_1)
          ) {
            inputProcessor.buyUnit(Castle::class.createHandInstance())
          }

          button(
            tooltip = "Buy Peasant",
            up = Hex.assets.peasant.getKeyFrame(0f),
            disableCheck = { territory -> (territory?.capital?.balance ?: -1) < PEASANT_PRICE && island.isCurrentTeamAI() },
            keyShortcut = intArrayOf(Keys.NUM_2)
          ) {
            inputProcessor.buyUnit(Peasant::class.createHandInstance())
          }
          button(
            tooltip = "Undo",
            up = Hex.assets.undo,
            disableCheck = { !island.history.canUndo() && island.isCurrentTeamAI() },
            keyShortcut = intArrayOf(Keys.NUM_3)
          ) {
            island.history.undo()
          }
          button(
            tooltip = "Undo All",
            up = Hex.assets.undoAll,
            disableCheck = { !island.history.canUndo() && island.isCurrentTeamAI() },
            keyShortcut = intArrayOf(Keys.NUM_4)
          ) {
            island.history.undoAll()
          }
          button(
            tooltip = "Redo",
            up = Hex.assets.redo,
            disableCheck = { !island.history.canRedo() && island.isCurrentTeamAI() },
            keyShortcut = intArrayOf(Keys.NUM_5)
          ) {
            island.history.redo()
          }
          button(
            tooltip = "Surrender",
            up = Hex.assets.surrender,
            keyShortcut = intArrayOf(Keys.NUM_6)
          ) {
            if (Settings.confirmSurrender) {
              confirmSurrender.show(stage)
            } else {
              surrender()
            }
          }
          button(
            tooltip = "Settings",
            up = Hex.assets.settings,
            down = Hex.assets.settingsDown,
            keyShortcut = intArrayOf(Keys.NUM_7)
          ) {
            Hex.screen = SettingsScreen
          }

          if (Hex.trace) {
            button(
              tooltip = "Help",
              up = Hex.assets.help,
              down = Hex.assets.helpDown,
              keyShortcut = intArrayOf(Keys.NUM_8)
            ) {
              if (distress) {
                distress = false
                Gdx.app.debug("DISTRESS SIGNAL", "Im suck in here!")
                if (Hex.args.cheating) {
                  MessagesRenderer.publishMessage(ScreenText("You're asking for help when cheating!? Not a very good cheater are you?", color = Color.GOLD))
                }
              }
            }
          }
          visTextButton("End Turn") {
            labelCell.height(size)
            labelCell.minWidth(size)
            disableChecker[this] = { island.isCurrentTeamAI() }
            onInteract(stage, Keys.ENTER) {
              endTurn()
            }
          }
        }
      }
    }
  }

  private fun surrender() {
    modifier = SURRENDER
    island.surrender()
  }

  fun updateWinningTurn() {
    for ((window, action) in labelUpdater) {
      window.action()
    }
  }

  fun endTurn() {
    if (island.isCurrentTeamHuman() && Settings.confirmEndTurn) {
      val minCost = Peasant::class.createHandInstance().price
      val hexagons = island.hexagons
        .map { island.getData(it) }
        .filter { it.team == island.currentTeam && (it.piece is LivingPiece || it.piece is Capital) }
      for (data in hexagons) {

        val piece = data.piece
        if ((piece is Capital && piece.balance >= minCost) || (piece is LivingPiece && !piece.moved)) {
          confirmEndTurn.show(stageScreen.stage)
          return
        }
      }
    }
    island.endTurn(inputProcessor)
  }

  override fun render(delta: Float) {
    super.render(delta)
    debugRenderer.frameUpdate()
    if (!Hex.args.mapEditor) {
      frameUpdatable.frameUpdate()
    }
    stageScreen.render(delta)
    val territory = island.selected
    for ((button, check) in disableChecker) {
      button.isDisabled = check(territory)
    }
  }

  override fun show() {
    stageScreen.show()
    Hex.inputMultiplexer.addProcessor(gestureDetector)
    Hex.inputMultiplexer.addProcessor(inputProcessor)
    super.show()

    if (island.currentAI != null) {
      island.endTurn(inputProcessor)
    }
  }

  override fun hide() {
    stageScreen.hide()
    super.hide()
    Hex.inputMultiplexer.removeProcessor(gestureDetector)
    Hex.inputMultiplexer.removeProcessor(inputProcessor)
    LevelSelectScreen.updateSelectPreview(id, false, modifier)
    modifier = NOTHING
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    stageScreen.resize(width, height)
    frameUpdatable.resize(width, height)
  }

  override fun dispose() {
    super.dispose()
    stageScreen.dispose()
  }

  companion object {
    val PEASANT_PRICE = Peasant::class.createHandInstance().price
    val CASTLE_PRICE = Castle::class.createHandInstance().price
    private var distress = true
  }
}
