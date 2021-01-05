package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.util.OsUtils
import com.kotcrab.vis.ui.widget.ButtonBar
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.isShown
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
import no.elg.hex.hexagon.CASTLE_PRICE
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.PEASANT_PRICE
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.GameInfoRenderer
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.LOST
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.NOTHING
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.SURRENDER
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.WON
import no.elg.hex.util.canAttack
import no.elg.hex.util.createHandInstance
import no.elg.hex.util.getData
import no.elg.hex.util.hide
import no.elg.hex.util.onAnyKeysDownEvent
import no.elg.hex.util.onInteract
import no.elg.hex.util.serialize
import no.elg.hex.util.show

/** @author Elg */
@Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
class PlayableIslandScreen(id: Int, island: Island) : PreviewIslandScreen(id, island) {

  private val stageScreen = StageScreen()
  val inputProcessor by lazy { GameInputProcessor(this) }

  private val frameUpdatable by lazy { GameInfoRenderer(this) }
  private val debugRenderer: DebugInfoRenderer by lazy { DebugInfoRenderer(this) }

  private val confirmEndTurn: VisWindow
  private val confirmSurrender: VisWindow
  internal val acceptAISurrender: VisWindow
  private val youWon: VisWindow
  private val youLost: VisWindow

  private val buttonGroup: KHorizontalGroup

  private val disableChecker: MutableMap<Disableable, (Territory?) -> Boolean> = mutableMapOf()
  private val labelUpdater: MutableMap<KVisWindow, KVisWindow.() -> Unit> = mutableMapOf()

  private var modifier = NOTHING

  val stage: Stage get() = stageScreen.stage

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

          val order =
            when {
              OsUtils.isWindows() -> ButtonBar.WINDOWS_ORDER
              OsUtils.isMac() -> ButtonBar.OSX_ORDER
              else -> ButtonBar.LINUX_ORDER
            }.replace(" ", "").map { " $it" }.joinToString()

          buttonBar(order = order) {

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
          }
          centerWindow()
          onAnyKeysDownEvent(Keys.ESCAPE, Keys.BACK, catchEvent = true) {
            this@visWindow.fadeOut()
          }
          addCloseButton()
          pack()
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
        this@PlayableIslandScreen.modifier = modifier
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
        gameEnded()
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

          val size = Value.percentWidth(0.08f, this@table)

          val disableInteract: (Territory?) -> Boolean = {
            island.isCurrentTeamAI() ||
              youWon.isShown() ||
              youLost.isShown() ||
              acceptAISurrender.isShown() ||
              confirmEndTurn.isShown() ||
              confirmSurrender.isShown()
          }

          @Scene2dDsl
          fun interactButton(
            tooltip: String,
            up: TextureRegion,
            down: TextureRegion? = null,
            disabled: TextureRegion? = null,
            disableCheck: ((Territory?) -> Boolean) = disableInteract,
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

          interactButton(
            tooltip = "Buy Peasant",
            up = Hex.assets.peasant.getKeyFrame(0f),
            disableCheck = { (it?.capital?.balance ?: -1) < PEASANT_PRICE || disableInteract(it) },
            keyShortcut = intArrayOf(Keys.NUM_1)
          ) {
            inputProcessor.buyUnit(Peasant::class.createHandInstance())
          }

          interactButton(
            tooltip = "Buy Castle",
            up = Hex.assets.castle,
            disableCheck = { (it?.capital?.balance ?: -1) < CASTLE_PRICE || disableInteract(it) },
            keyShortcut = intArrayOf(Keys.NUM_2)
          ) {
            inputProcessor.buyUnit(Castle::class.createHandInstance())
          }

          interactButton(
            tooltip = "Undo",
            up = Hex.assets.undo,
            disableCheck = { !island.history.canUndo() || disableInteract(it) },
            keyShortcut = intArrayOf(Keys.NUM_3)
          ) {
            island.history.undo()
          }
          interactButton(
            tooltip = "Undo All",
            up = Hex.assets.undoAll,
            disableCheck = { !island.history.canUndo() || disableInteract(it) },
            keyShortcut = intArrayOf(Keys.NUM_4)
          ) {
            island.history.undoAll()
          }
          interactButton(
            tooltip = "Redo",
            up = Hex.assets.redo,
            disableCheck = { !island.history.canRedo() || disableInteract(it) },
            keyShortcut = intArrayOf(Keys.NUM_5)
          ) {
            island.history.redo()
          }
          interactButton(
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
          interactButton(
            tooltip = "Settings",
            up = Hex.assets.settings,
            down = Hex.assets.settingsDown,
            keyShortcut = intArrayOf(Keys.NUM_7)
          ) {
            Hex.screen = Hex.settingsScreen
          }

          interactButton(
            tooltip = "Help",
            up = Hex.assets.help,
            down = Hex.assets.helpDown,
            keyShortcut = intArrayOf(Keys.NUM_8)
          ) {
            if (distress) {
              distress = false
              Gdx.app.debug("DISTRESS SIGNAL", "Im suck in here!")
              if (Hex.args.cheating) {
                MessagesRenderer.publishMessage("You're asking for help when cheating!? Not a very good cheater are you?", color = Color.GOLD)
              }
            }
            Hex.screen = Hex.tutorialScreen
          }

          visTextButton("End Turn") {
            labelCell.height(size)
            labelCell.minWidth(size)
            disableChecker[this] = { disableInteract(null) }
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

  private fun gameEnded() {
    for ((window, action) in labelUpdater) {
      window.action()
    }
  }

  fun checkEndedGame(): Boolean {
    val capitalCount = island.hexagons.count { island.getData(it).piece is Capital }
    if (capitalCount <= 1) {
      gameEnded()
      if (island.isCurrentTeamHuman()) {
        youWon.show(stage)
      } else {
        youLost.show(stage)
      }
      return true
    }
    return false
  }

  fun endTurn() {
    if (island.isCurrentTeamHuman() && Settings.confirmEndTurn) {
      val minCost = Peasant::class.createHandInstance().price

      for (hexagon in island.hexagons) {
        val data = island.getData(hexagon)
        val piece = data.piece
        if (data.team != island.currentTeam || (piece is Capital && piece.balance >= minCost)) continue
        else if (piece is LivingPiece) {
          if (piece.moved) continue
          val territory = island.findTerritory(hexagon) ?: error("Piece $piece has not moved and is in not in a territory")
          if (territory.enemyBorderHexes.none { hex -> island.canAttack(hex, piece) } &&
            territory.capital.balance < PEASANT_PRICE &&
            territory.hexagons.none {
              val terrPiece = island.getData(it).piece
              if (terrPiece === piece) false
              else terrPiece is LivingPiece && piece.canMerge(terrPiece)
            }
          ) {
            // The current piece is able to move, but not attack any territory, nor buy any new pieces to merge with
            continue
          }
        } else {
          continue
        }
        confirmEndTurn.show(stageScreen.stage)
        return
      }
    }
    island.endTurn(inputProcessor)
  }

  override fun render(delta: Float) {
    super.render(delta)
    debugRenderer.frameUpdate()
    frameUpdatable.frameUpdate()
    stageScreen.render(delta)
    val territory = island.selected
    for ((button, check) in disableChecker) {
      button.isDisabled = check(territory)
    }
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    stageScreen.resize(width, height)
    frameUpdatable.resize(width, height)
  }

  override fun show() {
    stageScreen.show()
    inputProcessor.show()
    super.show()

    if (island.isCurrentTeamAI()) {
      island.beginTurn(inputProcessor)
    }
  }

  override fun dispose() {
    super.dispose()
    frameUpdatable.dispose()
    stageScreen.dispose()
    if (modifier == NOTHING) {
      saveProgress()
    }

    LevelSelectScreen.updateSelectPreview(id, false, modifier, island)
    modifier = NOTHING
  }

  fun saveProgress() {
    Gdx.app.debug("IS PROGRESS", "Saving progress of island $id")
    islandPreferences.putString(getPrefName(id, false), island.serialize())
    islandPreferences.flush()
  }

  fun clearProgress() {
    Gdx.app.debug("IS PROGRESS", "Clearing progress of island $id")
    islandPreferences.remove(getPrefName(id, false))
    islandPreferences.remove(getPrefName(id, true))
    islandPreferences.flush()
  }

  companion object {
    val islandPreferences: Preferences by lazy { Gdx.app.getPreferences("island") }

    fun getProgress(id: Int, preview: Boolean = false): String? {
      val pref = getPrefName(id, preview)
      return islandPreferences.getString(pref, null)
    }

    private fun getPrefName(id: Int, preview: Boolean) = "$id${if (preview) "-preview" else ""}"

    private var distress = true
  }
}
