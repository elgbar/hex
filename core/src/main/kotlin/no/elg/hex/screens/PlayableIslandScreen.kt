package no.elg.hex.screens

import com.kotcrab.vis.ui.widget.ButtonBar
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.isShown
import ktx.actors.onClick
import ktx.scene2d.actors
import ktx.scene2d.scene2d
import ktx.scene2d.vis.buttonBar
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visWindow
import no.elg.hex.Hex
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.GameInfoRenderer
import no.elg.hex.input.BasicIslandInputProcessor
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.util.createHandInstance
import no.elg.hex.util.getData

/** @author Elg */
class PlayableIslandScreen(id: Int, island: Island) : PreviewIslandScreen(id, island) {

  private val stage = StageScreen()
  val basicIslandInputProcessor: BasicIslandInputProcessor by lazy {
    BasicIslandInputProcessor(this)
  }
  val inputProcessor by lazy { GameInputProcessor(this) }

  private val frameUpdatable by lazy { GameInfoRenderer(this) }
  private val debugRenderer: DebugInfoRenderer by lazy { DebugInfoRenderer(this) }

  val confirmEndTurn: VisWindow

  init {
    stage.stage.actors {
      confirmEndTurn = visWindow("Confirm End Turn") {
        isMovable = false
        isModal = true
        hide()

        visLabel("Are you sure you want to end turn?")
        row()

        buttonBar {
          setButton(
            ButtonBar.ButtonType.YES,
            scene2d.visTextButton("Yes") {
              onClick {
                println("end turn")
                island.endTurn(inputProcessor)
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
  }

  override fun show() {
    stage.show()
    Hex.inputMultiplexer.addProcessor(basicIslandInputProcessor)
    Hex.inputMultiplexer.addProcessor(inputProcessor)

    if (island.currentAI != null) {
      island.endTurn(inputProcessor)
    }
  }

  override fun hide() {
    super.hide()
    stage.hide()
    Hex.inputMultiplexer.removeProcessor(basicIslandInputProcessor)
    Hex.inputMultiplexer.removeProcessor(inputProcessor)
    LevelSelectScreen.updateSelectPreview(id, false)
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    stage.resize(width, height)
    frameUpdatable.resize(width, height)
    confirmEndTurn.centerWindow()
  }
}
