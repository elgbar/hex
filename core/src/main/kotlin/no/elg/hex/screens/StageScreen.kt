package no.elg.hex.screens

import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Scaling.fit
import com.badlogic.gdx.utils.viewport.ScalingViewport
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.visTable
import no.elg.hex.Hex
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/** @author Elg */
open class StageScreen(val useRootTable: Boolean = true) : AbstractScreen(false) {

  val stage by lazy { Stage(ScalingViewport(fit, 1f, 1f, camera)) }

  val rootTable: KVisTable get() = internalRootTable
  private lateinit var internalRootTable: KVisTable

  @Scene2dDsl
  @OptIn(ExperimentalContracts::class)
  inline fun rootTable(init: (@Scene2dDsl KVisTable).() -> Unit) {
    contract { callsInPlace(init, EXACTLY_ONCE) }
    require(useRootTable) { "Root table not enabled" }
    rootTable.init()
  }

  init {
    if (Hex.debugStage) {
      stage.isDebugAll = true
    }
    if (useRootTable) {
      stage.actors {
        internalRootTable = visTable {
          defaults().pad(20f)
          defaults().space(20f)
          setFillParent(true)
          setRound(false)
        }
      }
    }
  }

  override fun render(delta: Float) {
    with(stage.viewport) {
      HdpiUtils.glViewport(screenX, screenY, screenWidth, screenHeight)
    }
    stage.act()
    stage.draw()
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(stage)
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    stage.viewport.setWorldSize(width.toFloat(), height.toFloat())
    stage.viewport.update(width, height, true)
    updateCamera()

    for (actor in stage.actors) {
      if (actor is VisWindow) {
        actor.centerWindow()
      }
    }
  }

  override fun dispose() {
    super.dispose()
    stage.dispose()
  }
}