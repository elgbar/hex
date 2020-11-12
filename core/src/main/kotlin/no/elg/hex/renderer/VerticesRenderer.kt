package no.elg.hex.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.getData

class VerticesRenderer(private val islandScreen: PreviewIslandScreen) :
  FrameUpdatable, Disposable {

  private val mesh: Mesh =
    Mesh(
      true,
      MAX_VERTS,
      0,
      VertexAttribute(Usage.Position, POSITION_COMPONENTS, "a_position"),
      VertexAttribute(Usage.ColorPacked, 4, "a_color"))

  private val shader: ShaderProgram =
    {
      val fragShader: String = Gdx.files.internal(FRAG_SHADER_PATH).readString()
      val vertShader: String = Gdx.files.internal(VERT_SHADER_PATH).readString()

      ShaderProgram.pedantic = false
      val shader = ShaderProgram(vertShader, fragShader)
      val log = shader.log
      if (!shader.isCompiled) {
        throw GdxRuntimeException(log)
      }
      if (log != null && log.isNotEmpty()) {
        Gdx.app.log("Shader Log", log)
      }
      shader
    }()

  // The array which holds all the data, interleaved like so:
  //    x, y, r, g, b, a
  //    x, y, r, g, b, a,
  //    x, y, r, g, b, a,
  //    ... etc ...
  private val verts = FloatArray(MAX_VERTS * NUM_COMPONENTS)

  // The index position
  private var idx = 0

  override fun frameUpdate() {
    Gdx.gl.glEnable(GL20.GL_BLEND)
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

    val currHex = islandScreen.cursorHexagon

    // Render the hexagons
    for (hexagon in islandScreen.island.hexagons) {
      val data: HexagonData = islandScreen.island.getData(hexagon)
      if (data.invisible) continue
      val brightness =
        HexagonData.BRIGHTNESS +
          if (hexagon.cubeCoordinate == currHex?.cubeCoordinate) HexagonData.SELECTED else 0f
      data.type.render(this, data.color, brightness, hexagon)
    }
    flush()
  }

  private fun flush() {
    // if we've already flushed
    if (idx == 0) {
      return
    }

    // sends our vertex data to the mesh
    mesh.setVertices(verts)

    // no need for depth...
    //        Gdx.gl.glDepthMask(false);

    // enable blending, for alpha

    // number of vertices we need to render
    val vertexCount = idx / NUM_COMPONENTS

    // start the shader before setting any uniforms
    shader.begin()

    // update the projection matrix so our triangles are rendered in 2D
    shader.setUniformMatrix("u_projTrans", islandScreen.camera.combined)

    // render the mesh
    mesh.render(shader, GL20.GL_TRIANGLES, 0, vertexCount)
    shader.end()

    // re-enable depth to reset states to their default
    //        Gdx.gl.glDepthMask(true);

    // reset index to zero
    idx = 0
  }

  /**
   * @param cBit Color bit
   * @param verts Vertices to add
   */
  fun drawTriangle(cBit: Float, verts: FloatArray) {
    // we don't want to hit any index out of bounds exception...
    // so we need to flush the batch if we can't store any more verts
    if (idx == this.verts.size) {
      flush()
    }

    // now we push the vertex data into our array
    // we are assuming (0, 0) is lower left, and Y is up

    // bottom left vertex
    this.verts[idx++] = verts[0] // Position x
    this.verts[idx++] = verts[1] // Position y
    this.verts[idx++] = cBit // Color

    // top left vertex
    this.verts[idx++] = verts[2]
    this.verts[idx++] = verts[3]
    this.verts[idx++] = cBit

    // bottom right vertex
    this.verts[idx++] = verts[4]
    this.verts[idx++] = verts[5]
    this.verts[idx++] = cBit
  }

  override fun dispose() {
    mesh.dispose()
    shader.dispose()
  }

  companion object {
    private const val SHADERS_FOLDER = "shaders"

    private const val FRAG_SHADER_PATH = "${SHADERS_FOLDER}/hex.frag.glsl"
    private const val VERT_SHADER_PATH = "${SHADERS_FOLDER}/hex.vert.glsl"

    // Position attribute - (x, y)
    private const val POSITION_COMPONENTS = 2

    // Color attribute - (r, g, b, a)
    private const val COLOR_COMPONENTS = 1

    // Total number of components for all attributes
    private const val NUM_COMPONENTS = POSITION_COMPONENTS + COLOR_COMPONENTS

    // The maximum number of triangles our mesh will hold
    private const val MAX_TRIS =
      6 * 64 // each hexagon has 4 to 6 vertices, and there will be a least 64 hexagons

    // The maximum number of vertices our mesh will hold
    private const val MAX_VERTS = MAX_TRIS * 3
  }
}
