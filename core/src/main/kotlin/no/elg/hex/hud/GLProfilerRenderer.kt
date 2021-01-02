package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenDrawPosition.CENTER_LEFT

object GLProfilerRenderer : FrameUpdatable, Disposable {

  private val FLOAT_FORMAT: (Float) -> String = { "%.2f".format(it) }

  private val profiler = GLProfiler(Gdx.graphics)

  override fun frameUpdate() {
    if (profiler.isEnabled) {

      ScreenRenderer.drawAll(
        variableText("GL calls ", profiler.calls, 0, 450),
        variableText("Draw calls ", profiler.drawCalls, 0, 30),
        variableText("Shader Switches ", profiler.shaderSwitches, 0, 10),
        variableText("Texture Bindings ", profiler.textureBindings, 0, 20),
        variableText("Avg vertex count ", profiler.vertexCount.average, 0f, 1000f, format = FLOAT_FORMAT),
        variableText("GL Errors ", Gdx.gl.glGetError(), 0, 1),
        position = CENTER_LEFT
      )
      profiler.reset()
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
