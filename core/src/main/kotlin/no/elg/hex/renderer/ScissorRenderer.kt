package no.elg.hex.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import ktx.assets.disposeSafely
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.api.Resizable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class ScissorRenderer : FrameUpdatable, Disposable, Resizable {

  private var fbo: FrameBuffer? = null
  private val batch = SpriteBatch()

  protected var clearFbo = true
  protected var fboWidth: Int = 0
  protected var fboHeight: Int = 0

  fun drawFbo(x: Float, y: Float) {
    fbo?.colorBufferTexture?.also { texture ->
      batch.use {
        it.draw(texture, x, y)
      }
    }
  }

  @OptIn(ExperimentalContracts::class)
  protected fun use(block: (FrameBuffer) -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    fbo?.use {
      if (clearFbo) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        clearFbo = false
      }
      // https://www.khronos.org/opengl/wiki/Scissor_Test
      Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
      try {
        block(it)
      } finally {
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
      }
    }
    Hex.resetClearColor()
  }

  abstract fun calculateFboWidth(width: Int, height: Int): Int

  abstract fun calculateFboHeight(width: Int, height: Int): Int

  override fun resize(width: Int, height: Int) {
    batch.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
    fboWidth = calculateFboWidth(width, height).coerceAtLeast(1)
    fboHeight = calculateFboHeight(width, height).coerceAtLeast(1)

    fbo?.disposeSafely()
    fbo = FrameBuffer(Pixmap.Format.RGBA4444, fboWidth, fboHeight, false)
    clearFbo = true
  }

  override fun dispose() {
    fbo.disposeSafely()
    batch.disposeSafely()
  }
}