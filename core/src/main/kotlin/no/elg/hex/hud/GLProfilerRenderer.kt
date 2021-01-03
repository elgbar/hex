package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenDrawPosition.CENTER_LEFT

object GLProfilerRenderer : FrameUpdatable, Disposable {

  private val profiler = GLProfiler(Gdx.graphics)
  private val texts = arrayOf(
    variableText("GL calls ", profiler::getCalls, 0, 450),
    variableText("GL Draw calls ", profiler::getDrawCalls, 0, 30),
    variableText("GL Shader Switches ", profiler::getShaderSwitches, 0, 10),
    variableText("GL Texture Bindings ", profiler::getTextureBindings, 0, 20),
    variableText("GL Avg vertex count ", profiler.vertexCount::average, 0f, 1000f, format = { "%.2f".format(it) }),
    variableText("GL Errors ", Gdx.gl::glGetError, 0, 1),
    variableText("Screen Renderer Batches ", ScreenRenderer::draws, 0, 4),
  )

  override fun frameUpdate() {
    if (profiler.isEnabled) {
      ScreenRenderer.drawAll(*texts, position = CENTER_LEFT)
      profiler.reset()
      ScreenRenderer.resetDraws()
    }
  }

  fun enable() {
    profiler.enable()
  }

  fun disable() {
    profiler.disable()
  }

  override fun dispose() {
    profiler.disable()
  }
}
