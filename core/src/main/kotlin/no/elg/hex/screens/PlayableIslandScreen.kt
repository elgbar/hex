package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.isShown
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.table
import ktx.scene2d.vis.KVisImageButton
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.visImageButton
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.event.SimpleEventListener
import no.elg.hex.event.events.SettingsChangeEvent
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.GameInfoRenderer
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.input.GameInteraction
import no.elg.hex.island.Island
import no.elg.hex.island.Island.Companion.NEVER_PLAYED
import no.elg.hex.island.Territory
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.preview.PreviewModifier
import no.elg.hex.preview.PreviewModifier.AI_DONE
import no.elg.hex.preview.PreviewModifier.LOST
import no.elg.hex.preview.PreviewModifier.NOTHING
import no.elg.hex.preview.PreviewModifier.SURRENDER
import no.elg.hex.preview.PreviewModifier.WON
import no.elg.hex.renderer.DebugGraphRenderer
import no.elg.hex.util.actionableHexagons
import no.elg.hex.util.confirmWindow
import no.elg.hex.util.createHandInstance
import no.elg.hex.util.fill
import no.elg.hex.util.hide
import no.elg.hex.util.okWindow
import no.elg.hex.util.onInteract
import no.elg.hex.util.safeGetDelegate
import no.elg.hex.util.saveIslandProgress
import no.elg.hex.util.show

/** @author Elg */
class PlayableIslandScreen(metadata: FastIslandMetadata, island: Island) : PreviewIslandScreen(metadata, island, isPreviewRenderer = false) {

  private val stageScreen = StageScreen()
  private val inputProcessor by lazy { GameInputProcessor(this) }

  private val frameUpdatable by lazy { GameInfoRenderer(this) }
  private val debugFPSGraphRenderer by lazy { DebugGraphRenderer() }
  private val debugRenderer: DebugInfoRenderer by lazy { DebugInfoRenderer(this) }

  private val confirmEndTurn: VisWindow
  private val confirmSurrender: VisWindow
  internal val acceptAISurrender: VisWindow
  private val youWon: KVisWindow
  private val youLost: KVisWindow
  private val aiDone: KVisWindow

  private val windows get() = sequenceOf(confirmEndTurn, confirmSurrender, acceptAISurrender, youWon, youLost, aiDone)

  private val buttonGroup: VisTable

  private val disableChecker: MutableMap<Disableable, (Territory?) -> Boolean> = mutableMapOf()
  private val labelUpdater: MutableMap<KVisWindow, KVisWindow.() -> Unit> = mutableMapOf()

  private lateinit var toggleMusicButton: KVisImageButton

  private val settingsChangeEventListener = SimpleEventListener.create<SettingsChangeEvent<Boolean>> { (delegate, _, _) ->
    if (delegate == Settings::musicPaused.safeGetDelegate()) {
      toggleMusicButton.style.imageUp = TextureRegionDrawable(Hex.music.icon)
      toggleMusicButton.style.imageDown = TextureRegionDrawable(Hex.music.iconSelected)
      toggleMusicButton.style.imageOver = TextureRegionDrawable(Hex.music.iconSelected)
    }
  }

  val stage: Stage get() = stageScreen.stage

  init {
    island.gameInteraction = GameInteraction(island, endGame = ::endGame)
    stage.actors {

      val toLevelSelectScreen: KVisWindow.() -> Unit = {
        Hex.screen = LevelSelectScreen()
      }

      youWon = okWindow("You Won!", labelUpdater, toLevelSelectScreen) {
        onGameEnded(WON)
        "Congratulations! You won in ${island.round} rounds"
      }
      youLost = okWindow("You Lost", labelUpdater, toLevelSelectScreen) {
        onGameEnded(LOST)
        "Too bad! You lost in ${island.round} rounds to ${island.winningTeam.name}"
      }
      aiDone = okWindow("Game Over", labelUpdater, toLevelSelectScreen) {
        onGameEnded(AI_DONE)
        "The AI ${island.winningTeam.name} won in ${island.round} rounds"
      }

      confirmEndTurn = confirmWindow(
        "Confirm End Turn",
        "There still are actions to perform.\nAre you sure you want to end your turn?",
        whenDenied = { tempShowActionToDo = true }
      ) {
        tempShowActionToDo = false
        island.endTurn()
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

        island.fill(island.currentTeam)
        endGame(true)
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
            island.isCurrentTeamAI() || windows.any(Actor::isShown) || island.history.disabled
          }

          @Scene2dDsl
          fun <T> KWidget<T>.interactButton(
            tooltip: String,
            up: TextureRegion,
            down: TextureRegion? = null,
            disabled: TextureRegion? = null,
            playClick: Boolean = false,
            disableCheck: ((Territory?) -> Boolean) = { interactDisabled() },
            vararg keyShortcut: Int,
            onClick: Button.() -> Unit
          ): KVisImageButton {
            fun drawableToTextureRegion(drawable: TextureRegion): Drawable {
              val region = TextureRegion(drawable)
              region.flip(false, true)
              return TextureRegionDrawable(region)
            }

            return visImageButton {
              pad(10f)

              visTextTooltip(tooltip)
              style.imageUp = drawableToTextureRegion(up)

              if (down != null) {
                style.imageDown = drawableToTextureRegion(down)
                style.imageOver = drawableToTextureRegion(down)
              }
              if (disabled != null) {
                style.disabled = drawableToTextureRegion(disabled)
              }
              onInteract(stage, *keyShortcut, playClick = playClick, interaction = onClick)
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

            fun buyDisable(cost: Int): ((Territory?) -> Boolean) =
              { territory ->
                territory == null || (!island.gameInteraction.cheating && territory.capital.balance < cost) || interactDisabled()
              }

            interactButton(
              tooltip = "Buy Peasant",
              up = Hex.assets.peasant.getKeyFrame(0f),
              disableCheck = buyDisable(PEASANT_PRICE),
              keyShortcut = intArrayOf(Keys.NUM_1)
            ) {
              island.gameInteraction.buyUnit(Peasant::class.createHandInstance())
            }

            interactButton(
              tooltip = "Buy Castle",
              up = Hex.assets.castle,
              disableCheck = buyDisable(CASTLE_PRICE),
              keyShortcut = intArrayOf(Keys.NUM_2)
            ) {
              island.gameInteraction.buyUnit(Castle::class.createHandInstance())
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
              keyShortcut = intArrayOf(Keys.NUM_4)
            ) {
              island.history.redo()
            }
            interactButton(
              tooltip = "Undo All",
              up = Hex.assets.undoAll,
              disableCheck = { interactDisabled() || !island.history.canUndo() },
              keyShortcut = intArrayOf(Keys.NUM_5)
            ) {
              island.history.undoAll()
            }

            interactButton(
              tooltip = "Surrender",
              up = Hex.assets.surrender,
              playClick = true,
              disableCheck = { if (island.onlyAIPlayers) false else interactDisabled() },
              keyShortcut = intArrayOf(Keys.NUM_6)
            ) {
              if (Settings.confirmSurrender) {
                showWindow(confirmSurrender)
              } else {
                surrender()
              }
            }

            toggleMusicButton = interactButton(
              tooltip = "Toggle music",
              up = Hex.music.icon,
              down = Hex.music.iconSelected,
              playClick = true,
              disableCheck = { false },
              keyShortcut = intArrayOf(Keys.NUM_7)
            ) {
              Settings.musicPaused = !Settings.musicPaused
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

  private fun showWindow(window: VisWindow) {
    if (window is KVisWindow) {
      labelUpdater[window]?.also { action ->
        action.invoke(window)
      }
    }
    windows.filter { it != window }.forEach(VisWindow::hide)
    window.show(stage)
  }

  private fun onGameEnded(modifier: PreviewModifier) {
    island.history.disable()
    metadata.modifier = modifier
    if (modifier == WON && (metadata.userRoundsToBeat == NEVER_PLAYED || metadata.userRoundsToBeat > island.round)) {
      metadata.userRoundsToBeat = island.round
    }
    Hex.assets.islandPreviews.updateSelectPreview(metadata, island)
  }

  private fun surrender() {
    onGameEnded(SURRENDER)
    Hex.screen = LevelSelectScreen()
    Gdx.app.log("ISLAND", "Player surrendered on round ${island.round}")
  }

  private fun endGame(win: Boolean) {
    showWindow(
      when {
        island.onlyAIPlayers -> aiDone
        win -> youWon
        else -> youLost
      }
    )
  }

  private fun endTurn(allowAISurrender: Boolean = true) {
    tempShowActionToDo = false
    island.select(null)

    val currentTeam = island.currentTeam

    // we only ask for AI surrender when there is a single player
    // if there are no players (ai vs ai) we want to watch the whole thing
    // and if there are more than one player they should decide if they surrender
    if (Settings.allowAIToSurrender && allowAISurrender && island.singleAliveRealPlayer) {
      // surrender rules
      // either you own more than 75% of all hexagons
      // or all enemies have less than 12,5% of the hexagons
      val percentagesHexagons = island.calculatePercentagesHexagons()
      val teamPercentages = percentagesHexagons[currentTeam] ?: 0f
      if (teamPercentages >= PERCENT_HEXES_OWNED_TO_WIN ||
        percentagesHexagons.all { (team, percent) -> team === currentTeam || percent < MAX_PERCENT_HEXES_AI_OWN_TO_SURRENDER }
      ) {
        showWindow(acceptAISurrender)
        return
      }
    }

    if (island.isCurrentTeamHuman() && Settings.confirmEndTurn) {
      // only display the confirm button if the user have any action to do left
      if (island.actionableHexagons().any()) {
        // Let's warn users of ending their turn
        tempShowActionToDo = true
        showWindow(confirmEndTurn)
        return
      }
    }

    if (island.checkGameEnded()) {
      island.gameInteraction.endGame()
      return
    }
    saveIslandProgress()

    island.endTurn()
  }

  override fun render(delta: Float) {
    super.render(delta)
    if (DebugGraphRenderer.isEnabled) {
      debugFPSGraphRenderer.frameUpdate()
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
    debugFPSGraphRenderer.resize(width, height)
  }

  override fun show() {
    stageScreen.show()
    inputProcessor.show()
    super.show()
    Hex.music.playRandom()
  }

  override fun afterShown() {
    if (island.isCurrentTeamAI()) {
      island.beginTurn()
    }
  }

  override fun dispose() {
    super.dispose()
    frameUpdatable.dispose()
    stageScreen.dispose()
    island.cancelCurrentAI()
    if (metadata.modifier == NOTHING) {
      saveIslandProgress()
    }

    debugFPSGraphRenderer.dispose()
    debugRenderer.dispose()
    settingsChangeEventListener.dispose()
  }

  companion object {
    const val PERCENT_HEXES_OWNED_TO_WIN = 0.75f
    const val MAX_PERCENT_HEXES_AI_OWN_TO_SURRENDER = 0.125f

    val PEASANT_PRICE = Peasant::class.createHandInstance().price
    val CASTLE_PRICE = Castle::class.createHandInstance().price
  }
}