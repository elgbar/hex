package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.widget.ButtonBar
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.isShown
import ktx.actors.onChange
import ktx.actors.onChangeEvent
import ktx.actors.onClick
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.scene2d
import ktx.scene2d.table
import ktx.scene2d.vis.KVisImageButton
import ktx.scene2d.vis.buttonBar
import ktx.scene2d.vis.visImageButton
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visWindow
import no.elg.hex.Hex
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.GameInfoRenderer
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.util.createHandInstance
import no.elg.hex.util.getData
import no.elg.hex.util.toggleShown

/** @author Elg */
class PlayableIslandScreen(id: Int, island: Island) : PreviewIslandScreen(id, island) {

  val stage = StageScreen()
  val inputProcessor by lazy { GameInputProcessor(this) }

  private val frameUpdatable by lazy { GameInfoRenderer(this) }
  private val debugRenderer: DebugInfoRenderer by lazy { DebugInfoRenderer(this) }

  private val confirmEndTurn: VisWindow
  private val confirmSurrender: VisWindow
  private val disableChecker: MutableMap<KVisImageButton, (Territory?) -> Boolean> = mutableMapOf()

  private var surredered = false

  init {
    stage.stage.actors {
      if (Hex.args.`stage-debug` || Hex.trace) {
        stage.isDebugAll = true
      }
      @Scene2dDsl
      fun confirmWindow(title: String, text: String, whenConfirmed: () -> Unit): VisWindow {
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
                  whenConfirmed()
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

      confirmEndTurn = confirmWindow("Confirm End Turn", "Are you sure you want to end your turn?") {
        island.endTurn(inputProcessor)
      }
      confirmSurrender = confirmWindow("Confirm Surrender", "Are you sure you want to surrender?") {
        surredered = true
        island.surrender()
      }

      table {

        setFillParent(true)
        bottom()
        horizontalGroup {
          bottom()
          expand()
          space(20f)
          pad(20f)

          @Scene2dDsl
          fun button(
            up: TextureRegion,
            down: TextureRegion? = null,
            disabled: TextureRegion? = null,
            disableCheck: ((Territory?) -> Boolean)? = null,
            onClick: KVisImageButton.(event: ChangeEvent) -> Unit
          ) {

            fun drawableToTextureRegion(drawable: TextureRegion): Drawable {
              val region = TextureRegion(drawable)
              region.flip(false, true)
              return TextureRegionDrawable(region)
            }

            visImageButton {
              style.imageUp = drawableToTextureRegion(up)

              if (down != null) {
                style.imageDown = drawableToTextureRegion(down)
              }
              if (disabled != null) {
                style.disabled = drawableToTextureRegion(disabled)
              }
              onChangeEvent(onClick)
              if (disableCheck != null) {
                disableChecker[this] = disableCheck
              }

              background = null
              isTransform = true
              val size = Value.percentHeight(0.1f, this@table)
              imageCell.size(size)
            }
          }

          button(Hex.assets.castle, disableCheck = { territory -> (territory?.capital?.balance ?: -1) < CASTLE_PRICE }) {
            inputProcessor.buyUnit(Castle::class.createHandInstance())
          }
          button(Hex.assets.peasant.getKeyFrame(0f), disableCheck = { territory -> (territory?.capital?.balance ?: -1) < PEASANT_PRICE }) {
            inputProcessor.buyUnit(Peasant::class.createHandInstance())
          }
          button(Hex.assets.undo) {
            island.history.undo()
          }
          button(Hex.assets.undoAll) {
            island.history.undoAll()
          }
          button(Hex.assets.redo) {
            island.history.redo()
          }
          button(Hex.assets.surrender) {
            confirmSurrender.toggleShown(stage)
          }
          if (Hex.trace) {
            button(Hex.assets.settings, Hex.assets.settingsDown) {
            }
          }
          button(Hex.assets.help, Hex.assets.helpDown) {
            if (distress) {
              distress = false
              Gdx.app.debug("DISTRESS SIGNAL", "Im suck in here!")
              if (Hex.args.cheating) {
                MessagesRenderer.publishMessage("You're asking for help when cheating!? No a very good cheater are you?")
              }
            }
          }
          visTextButton("End Turn") {
            onChange {
              endTurn()
            }
          }
        }
      }
    }
  }

  fun endTurn() {
    if (island.currentAI == null) {
      val minCost = Peasant::class.createHandInstance().price
      for (
        data in island.hexagons
          .map { island.getData(it) }
          .filter { it.team == island.currentTeam && (it.piece is LivingPiece || it.piece is Capital) }
      ) {

        val piece = data.piece
        if ((piece is Capital && piece.balance >= minCost) || (piece is LivingPiece && !piece.moved)) {
          confirmEndTurn.centerWindow()
          if (!confirmEndTurn.isShown()) {
            stage.stage.addActor(confirmEndTurn.fadeIn())
          }
          return
        }
      }
    }
    island.endTurn(inputProcessor)
  }

  override fun render(delta: Float) {
    super.render(delta)
    if (Hex.debug) {
      debugRenderer.frameUpdate()
    }
    if (!Hex.args.mapEditor) {
      frameUpdatable.frameUpdate()
    }
    stage.render(delta)
    val territory = island.selected
    for ((button, check) in disableChecker) {
      button.isDisabled = check(territory)
    }
  }

  override fun show() {
    super.show()
    stage.show()
    Hex.inputMultiplexer.addProcessor(inputProcessor)

    if (island.currentAI != null) {
      island.endTurn(inputProcessor)
    }
  }

  override fun hide() {
    super.hide()
    stage.hide()
    Hex.inputMultiplexer.removeProcessor(inputProcessor)
    LevelSelectScreen.updateSelectPreview(id, false, surredered)
    surredered = false
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    stage.resize(width, height)
    frameUpdatable.resize(width, height)
    confirmEndTurn.centerWindow()
  }

  companion object {
    val PEASANT_PRICE = Peasant::class.createHandInstance().price
    val CASTLE_PRICE = Castle::class.createHandInstance().price
    var distress = true
  }
}
