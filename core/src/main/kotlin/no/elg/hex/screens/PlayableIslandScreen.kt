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
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.isShown
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.table
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.visImageButton
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
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
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.GameInfoRenderer
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.input.GameInteraction
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.preview.PreviewModifier
import no.elg.hex.preview.PreviewModifier.AI_DONE
import no.elg.hex.preview.PreviewModifier.LOST
import no.elg.hex.preview.PreviewModifier.NOTHING
import no.elg.hex.preview.PreviewModifier.SURRENDER
import no.elg.hex.preview.PreviewModifier.WON
import no.elg.hex.renderer.DebugGraphRenderer
import no.elg.hex.util.canAttack
import no.elg.hex.util.confirmWindow
import no.elg.hex.util.createHandInstance
import no.elg.hex.util.getData
import no.elg.hex.util.okWindow
import no.elg.hex.util.onInteract
import no.elg.hex.util.saveProgress
import no.elg.hex.util.show

/** @author Elg */
class PlayableIslandScreen(id: Int, island: Island) : PreviewIslandScreen(id, island, isPreviewRenderer = false) {

  private val stageScreen = StageScreen()
  private val inputProcessor by lazy { GameInputProcessor(this) }

  private val frameUpdatable by lazy { GameInfoRenderer(this) }
  private val debugRenderer: DebugInfoRenderer by lazy { DebugInfoRenderer(this) }

  private val confirmEndTurn: VisWindow
  private val confirmSurrender: VisWindow
  internal val acceptAISurrender: VisWindow
  private val youWon: VisWindow
  private val youLost: VisWindow
  private val aiDone: VisWindow

  private val buttonGroup: VisTable

  private val disableChecker: MutableMap<Disableable, (Territory?) -> Boolean> = mutableMapOf()
  private val labelUpdater: MutableMap<KVisWindow, KVisWindow.() -> Unit> = mutableMapOf()

  val stage: Stage get() = stageScreen.stage

  init {
    island.gameInteraction = GameInteraction(island, endGame = ::endGame)
    stageScreen.stage.actors {

      fun endGame(modifier: PreviewModifier): KVisWindow.() -> Unit = {
        island.history.disable()
        metadata.modifier = modifier
        island.restoreInitialState()
      }

      youWon = okWindow("You Won!", labelUpdater, endGame(WON)) { "Congratulations! You won in ${island.round} rounds" }
      youLost = okWindow("You Lost", labelUpdater, endGame(LOST)) { "Too bad! You lost in ${island.round} rounds to ${island.winningTeam.name}" }
      aiDone = okWindow("Game Over", labelUpdater, endGame(AI_DONE)) { "The AI ${island.winningTeam.name} won in ${island.round} rounds" }

      confirmEndTurn = confirmWindow(
        "Confirm End Turn",
        "There still are actions to perform.\nAre you sure you want to end your turn?"
      ) {
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

        for (hexagon in island.visibleHexagons) {
          val data = island.getData(hexagon)
          if (data.team == island.currentTeam) {
            continue
          }
          data.team = island.currentTeam
          data.setPiece<Empty>()
        }

        island.select(null)
        island.select(island.visibleHexagons.first())
        island.select(null)
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
            playClick: Boolean = false,
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

            fun buyDisable(cost: Int): ((Territory?) -> Boolean) = { territory ->
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
              playClick = true,
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

  private fun endGame(win: Boolean) {
    for ((window, action) in labelUpdater) {
      window.action()
    }
    when {
      island.realPlayers == 0 -> aiDone.show(stage)
      win -> youWon.show(stage)
      else -> youLost.show(stage)
    }
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
      val teamPercentages = percentagesHexagons[currentTeam] ?: 0f
      if (teamPercentages >= PERCENT_HEXES_OWNED_TO_WIN ||
        percentagesHexagons.all { (team, percent) -> team === currentTeam || percent < MAX_PERCENT_HEXES_AI_OWN_TO_SURRENDER }
      ) {
        acceptAISurrender.show(stageScreen.stage)
        return
      }
    }

    if (island.isCurrentTeamHuman() && Settings.confirmEndTurn) {
      // only display the confirm button if the user have any action to do left
      for (hexagon in island.visibleHexagons) {
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

    if (island.checkGameEnded()) {
      island.gameInteraction.endGame()
      return
    }
    saveProgress()

    island.endTurn()
  }

  override fun render(delta: Float) {
    super.render(delta)
    if (DebugGraphRenderer.isEnabled) {
      DebugGraphRenderer.frameUpdate()
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
  }

  override fun show() {
    stageScreen.show()
    inputProcessor.show()
    super.show()

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
      saveProgress()
    }

    DebugGraphRenderer.dispose()
    debugRenderer.dispose()
  }

  companion object {
    const val PERCENT_HEXES_OWNED_TO_WIN = 0.75f
    const val MAX_PERCENT_HEXES_AI_OWN_TO_SURRENDER = 0.125f

    val PEASANT_PRICE = Peasant::class.createHandInstance().price
    val CASTLE_PRICE = Castle::class.createHandInstance().price
  }
}