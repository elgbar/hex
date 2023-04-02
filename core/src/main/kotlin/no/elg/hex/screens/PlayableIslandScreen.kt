package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.util.OsUtils
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.isShown
import ktx.actors.onClick
import ktx.assets.disposeSafely
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.table
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.visImageButton
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import ktx.scene2d.vis.visWindow
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.hexagon.BARON_STRENGTH
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.KNIGHT_STRENGTH
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.PEASANT_STRENGTH
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.SPEARMAN_STRENGTH
import no.elg.hex.hexagon.SimpleEventListener
import no.elg.hex.hexagon.TeamChangeHexagonDataEvent
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.GameInfoRenderer
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.renderer.DebugGraphRenderer
import no.elg.hex.renderer.StrengthBarRenderer
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.AI_DONE
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.LOST
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.NOTHING
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.SURRENDER
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.WON
import no.elg.hex.util.canAttack
import no.elg.hex.util.createHandInstance
import no.elg.hex.util.getData
import no.elg.hex.util.hide
import no.elg.hex.util.isLazyInitialized
import no.elg.hex.util.onAnyKeysDownEvent
import no.elg.hex.util.onInteract
import no.elg.hex.util.show

/** @author Elg */
@Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
class PlayableIslandScreen(id: Int, island: Island) : PreviewIslandScreen(id, island) {

  private lateinit var listener: SimpleEventListener<TeamChangeHexagonDataEvent>
  private val stageScreen = StageScreen()
  val inputProcessor by lazy { GameInputProcessor(this) }

  private val frameUpdatable by lazy { GameInfoRenderer(this) }
  private val debugRenderer: DebugInfoRenderer by lazy { DebugInfoRenderer(this) }
  private val strengthBarRenderer by lazy { StrengthBarRenderer(this.island) }

  private val confirmEndTurn: VisWindow
  private val confirmSurrender: VisWindow
  internal val acceptAISurrender: VisWindow
  private val youWon: VisWindow
  private val youLost: VisWindow
  private val aiDone: VisWindow

  private val buttonGroup: VisTable

  private val disableChecker: MutableMap<Disableable, (Territory?) -> Boolean> = mutableMapOf()
  private val labelUpdater: MutableMap<KVisWindow, KVisWindow.() -> Unit> = mutableMapOf()

  private var modifier = NOTHING

  val stage: Stage get() = stageScreen.stage

  init {

    stageScreen.stage.actors {

      @Scene2dDsl
      fun confirmWindow(
        title: String,
        text: String,
        whenDenied: KVisWindow.() -> Unit = {},
        whenConfirmed: KVisWindow.() -> Unit
      ): VisWindow {
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
          onAnyKeysDownEvent(Keys.ESCAPE, Keys.BACK, catchEvent = true) {
            this@visWindow.fadeOut()
          }
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

      fun endGame(modifier: PreviewModifier): KVisWindow.() -> Unit = {
        island.history.disable()
        this@PlayableIslandScreen.modifier = modifier
        island.restoreInitialState()
      }

      youWon = okWindow("You Won!", { "Congratulations! You won in ${island.round} rounds" }, endGame(WON))
      youLost = okWindow("You Lost", { "Too bad! You lost in ${island.round} rounds to ${island.winningTeam.name}" }, endGame(LOST))
      aiDone = okWindow("Game Over", { "The AI ${island.winningTeam.name} won in ${island.round} rounds" }, endGame(AI_DONE))

      confirmEndTurn = confirmWindow(
        "Confirm End Turn",
        "There still are actions to perform.\nAre you sure you want to end your turn?"
      ) {
        island.endTurn(inputProcessor)
      }

      confirmSurrender = confirmWindow("Confirm Surrender", "Are you sure you want to surrender?") {
        surrender()
      }

      acceptAISurrender = confirmWindow(
        "Accept AI Surrender",
        "The AI wish to surrender, do you accept?",
        {
          endTurn(false)
        }
      ) {
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
        gameEnded(true)
      }

      table {

        setFillParent(true)
        bottom()
        buttonGroup = visTable(true) {
          it.left()
          it.bottom()

          it.fillX()
          it.expandX()
          it.spaceBottom(it.spaceBottomValue.get() * 2f)
          it.pad(20f)

          val size = Value.percentWidth(0.08f, this@table)

          val interactDisabled: () -> Boolean = {
            island.isCurrentTeamAI() ||
              youWon.isShown() ||
              youLost.isShown() ||
              aiDone.isShown() ||
              acceptAISurrender.isShown() ||
              confirmEndTurn.isShown() ||
              confirmSurrender.isShown() ||
              island.history.disabled
          }

          @Scene2dDsl
          fun <T> KWidget<T>.interactButton(
            tooltip: String,
            up: TextureRegion,
            down: TextureRegion? = null,
            disabled: TextureRegion? = null,
            disableCheck: ((Territory?) -> Boolean) = { interactDisabled() },
            vararg keyShortcut: Int,
            onClick: Button.() -> Unit
          ) {
            fun drawableToTextureRegion(drawable: TextureRegion): Drawable {
              val region = TextureRegion(drawable)
              region.flip(false, true)
              return TextureRegionDrawable(region)
            }

            visImageButton {
              pad(10f)

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
              isGenerateDisabledImage = true
              isFocusBorderEnabled = true
              isTransform = true
              imageCell.size(size)
            }
          }

          horizontalGroup { cell ->
            cell.space(30f)
            cell.expandX()
            cell.left()

            fun buyDisable(cost: Int): ((Territory?) -> Boolean) = { territory ->
              territory == null || (!inputProcessor.cheating && territory.capital.balance < cost) || interactDisabled()
            }

            interactButton(
              tooltip = "Buy Peasant",
              up = Hex.assets.peasant.getKeyFrame(0f),
              disableCheck = buyDisable(PEASANT_PRICE),
              keyShortcut = intArrayOf(Keys.NUM_1)
            ) {
              inputProcessor.buyUnit(Peasant::class.createHandInstance())
            }

            interactButton(
              tooltip = "Buy Castle",
              up = Hex.assets.castle,
              disableCheck = buyDisable(CASTLE_PRICE),
              keyShortcut = intArrayOf(Keys.NUM_2)
            ) {
              inputProcessor.buyUnit(Castle::class.createHandInstance())
            }
          }

          horizontalGroup { cell ->
            cell.expandX()
            cell.center()

            interactButton(
              tooltip = "Undo",
              up = Hex.assets.undo,
              disableCheck = { interactDisabled() || !island.history.canUndo() },
              keyShortcut = intArrayOf(Keys.NUM_3)
            ) {
              island.history.undo()
            }
            interactButton(
              tooltip = "Redo",
              up = Hex.assets.redo,
              disableCheck = { interactDisabled() || !island.history.canRedo() },
              keyShortcut = intArrayOf(Keys.NUM_5)
            ) {
              island.history.redo()
            }
            interactButton(
              tooltip = "Undo All",
              up = Hex.assets.undoAll,
              disableCheck = { interactDisabled() || !island.history.canUndo() },
              keyShortcut = intArrayOf(Keys.NUM_4)
            ) {
              island.history.undoAll()
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
          }

          horizontalGroup { cell ->
            cell.expandX()
            cell.right()

            visTextButton("End Turn") {

              labelCell.height(size)
              labelCell.minWidth(size)
              labelCell.right()

              disableChecker[this] = { interactDisabled() }
              onInteract(stage, Keys.ENTER) {
                endTurn()
              }
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

  internal fun gameEnded(win: Boolean) {
    for ((window, action) in labelUpdater) {
      window.action()
    }
    when {
      island.realPlayers == 0 -> aiDone.show(stage)
      win -> youWon.show(stage)
      else -> youLost.show(stage)
    }
  }

  fun checkEndedGame(): Boolean {
    val capitalCount = island.hexagons.count { island.getData(it).piece is Capital }
    if (capitalCount <= 1) {
      gameEnded(island.isCurrentTeamHuman())
      return true
    }
    return false
  }

  private fun endTurn(allowAISurrender: Boolean = true) {
    island.select(null)

    val currentTeam = island.currentTeam

    // we only ask for AI surrender when there is a single player
    // if there are no players (ai vs ai) we want to watch the whole thing
    // and if there are more than one player they should decide if they surrender
    if (Settings.allowAIToSurrender && allowAISurrender && island.realPlayers == 1) {
      // surrender rules
      // either you own more than 75% of all hexagons
      // or all enemies have less than 12,5% of the hexagons
      val percentagesHexagons = island.calculatePercentagesHexagons()
      if ((percentagesHexagons[currentTeam] ?: 0f) >= PERCENT_HEXES_OWNED_TO_WIN ||
        percentagesHexagons.all { (team, percent) -> team === currentTeam || percent < MAX_PERCENT_HEXES_AI_OWN_TO_SURRENDER }
      ) {
        acceptAISurrender.show(stageScreen.stage)
        return
      }
    }

    if (island.isCurrentTeamHuman() && Settings.confirmEndTurn) {
      // only display the confirm button if the user have any action to do left
      for (hexagon in island.hexagons) {
        val data = island.getData(hexagon)
        val piece = data.piece

        if (data.team != currentTeam) {
          // not our team
          continue
        } else if (piece is Capital) {
          val balance = piece.balance
          if (balance < PEASANT_PRICE) {
            // We cannot afford anything
            continue
          }

          val strength = when {
            balance < PEASANT_PRICE * 2 -> PEASANT_STRENGTH
            balance in PEASANT_PRICE * 2 until PEASANT_PRICE * 3 -> SPEARMAN_STRENGTH
            balance in PEASANT_PRICE * 3 until PEASANT_PRICE * 4 -> KNIGHT_STRENGTH
            else -> BARON_STRENGTH
          }

          val territory = island.findTerritory(hexagon)
          if (territory == null) {
            Gdx.app.error("ISLAND", "Hexagon ${hexagon.id} is not a part of a territory!")
            continue
          }
          val cannotBuyAndAttackAnything = territory.enemyBorderHexes.none { hex -> island.canAttack(hex, strength) }
          val cannotBuyAndPlaceCastle = balance < CASTLE_PRICE || territory.hexagons.none { hex -> island.getData(hex).piece is Empty }
          if (cannotBuyAndAttackAnything && cannotBuyAndPlaceCastle) {
            continue
          }
        } else if (piece is LivingPiece) {
          if (piece.moved) {
            // If the piece has moved, it cannot make another move!
            continue
          }

          val territory = island.findTerritory(hexagon)
          if (territory == null) {
            Gdx.app.error("ISLAND", "Hexagon ${hexagon.id} is not a part of a territory!")
            continue
          }
          val canNotAttackAnything = territory.enemyBorderHexes.none { hex -> island.canAttack(hex, piece) }
          val canNotMergeWithOtherPiece = territory.hexagons.none {
            val terrPiece = island.getData(it).piece
            return@none if (terrPiece === piece) {
              false // can never merge with self
            } else {
              terrPiece is LivingPiece && piece.canMerge(terrPiece)
            }
          }
          if (canNotAttackAnything && canNotMergeWithOtherPiece) {
            // The current piece is able to move, but not attack any territory, nor buy any new pieces to merge with
            continue
          }
        } else {
          // any other piece should be ignored
          continue
        }

        // We can do an action! Let's warn users of ending their turn
        confirmEndTurn.show(stageScreen.stage)
        return
      }
    }

    if (checkEndedGame()) {
      return
    }
    saveProgress()

    island.endTurn(inputProcessor)
  }

  override fun render(delta: Float) {
    super.render(delta)
    if (DebugGraphRenderer.isEnabled) {
      DebugGraphRenderer.frameUpdate()
    }
    if (StrengthBarRenderer.isEnabled) {
      strengthBarRenderer.frameUpdate()
    }
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
    if (DebugGraphRenderer.isEnabled) {
      DebugGraphRenderer.resize(width, height)
    }
    if (StrengthBarRenderer.isEnabled) {
      strengthBarRenderer.resize(width, height)
    }
  }

  override fun show() {
    stageScreen.show()
    inputProcessor.show()
    super.show()

    listener = SimpleEventListener.create {
      island.hexagonsPerTeam.getAndIncrement(it.old, 0, -1)
      island.hexagonsPerTeam.getAndIncrement(it.new, 0, 1)
    }
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
    listener.disposeSafely()

    DebugGraphRenderer.dispose()

    if (::strengthBarRenderer.isLazyInitialized) {
      strengthBarRenderer.dispose()
    }
    debugRenderer.dispose()
  }

  companion object {
    const val PERCENT_HEXES_OWNED_TO_WIN = 0.75f
    const val MAX_PERCENT_HEXES_AI_OWN_TO_SURRENDER = 0.125f

    private const val MOBILE_BUTTON_PADDING = 45f
    private const val DESKTOP_BUTTON_PADDING = 10f

    private const val MOBILE_SPACING = 20f
    private const val DESKTOP_SPACING = 5f

    val PEASANT_PRICE = Peasant::class.createHandInstance().price
    val CASTLE_PRICE = Castle::class.createHandInstance().price

    val buttonPadding: Float get() = if (OsUtils.isAndroid() || OsUtils.isIos()) MOBILE_BUTTON_PADDING else DESKTOP_BUTTON_PADDING
    val platformSpacing: Float get() = if (OsUtils.isAndroid() || OsUtils.isIos()) MOBILE_SPACING else DESKTOP_SPACING
  }
}