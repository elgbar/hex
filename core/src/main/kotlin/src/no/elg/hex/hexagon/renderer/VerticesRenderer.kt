package src.no.elg.hex.hexagon.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import no.elg.hex.Hex
import no.elg.hex.Hex.camera
import src.no.elg.hex.FrameUpdatable
import src.no.elg.hex.InputHandler
import src.no.elg.hex.hexagon.HexUtil
import src.no.elg.hex.hexagon.HexagonData

object VerticesRenderer : FrameUpdatable, Disposable {

  //@formatter:off //TODO read this from file?
  private const  val  VERT_SHADER = "attribute vec2 a_position;\n" +
    "attribute vec4 a_color;\n" +
    "uniform mat4 u_projTrans;\n" +
    "varying vec4 vColor;\n" +
    "void main() {\n" +
    "	vColor = a_color;\n" +
    "	gl_Position =  u_projTrans * vec4(a_position.xy, 0.0, 1.0);\n" +
    "}"
  private const  val  FRAG_SHADER = "#ifdef GL_ES\n" +
    "precision mediump float;\n" +
    "#endif\n" +
    "varying vec4 vColor;\n" +
    "void main() {\n" +
    "	gl_FragColor = vColor;\n" +
    "}"
  //@formatter:on

  //Position attribute - (x, y)
  private const val POSITION_COMPONENTS = 2

  //Color attribute - (r, g, b, a)
  private const val COLOR_COMPONENTS = 1

  //Total number of components for all attributes
  private const val NUM_COMPONENTS = POSITION_COMPONENTS + COLOR_COMPONENTS

  //The maximum number of triangles our mesh will hold
  private const val MAX_TRIS = 384 // each hexagon has 4 to 6 vertices, and there will be a least 64 hexagons

  //The maximum number of vertices our mesh will hold
  private const val MAX_VERTS = MAX_TRIS * 3


  private val mesh: Mesh = Mesh(true, MAX_VERTS, 0,
    VertexAttribute(Usage.Position, POSITION_COMPONENTS, "a_position"),
    VertexAttribute(Usage.ColorPacked, 4, "a_color"))
  private val shader: ShaderProgram = {
    ShaderProgram.pedantic = false
    val shader = ShaderProgram(VERT_SHADER, FRAG_SHADER)
    val log = shader.log
    if (!shader.isCompiled) {
      throw GdxRuntimeException(log)
    }
    if (log != null && log.length != 0) {
      println("Shader Log: $log")
    }
    shader
  }()

  //The array which holds all the data, interleaved like so:
  //    x, y, r, g, b, a
  //    x, y, r, g, b, a,
  //    x, y, r, g, b, a,
  //    ... etc ...
  private val verts = FloatArray(MAX_VERTS * NUM_COMPONENTS)

  //The index position
  private var idx = 0

  override fun frameUpdate() {
    Gdx.gl.glEnable(GL20.GL_BLEND)
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

    val currHex = InputHandler.cursorHex

    //Render the hexagons
    for (hexagon in HexUtil.getHexagons(Hex.world.grid)) {
      val data: HexagonData = HexUtil.getData(hexagon)
//      data.brightness = if (highlighted.contains(hexagon)) HexagonData.BRIGHT else HexagonData.DIM
      if (hexagon == currHex) {
        data.brightness += HexagonData.SELECTED
      }
      data.type.render(this, data.color, data.brightness, hexagon)
    }
    flush()
  }

  private fun flush() {
    //if we've already flushed
    if (idx == 0) {
      return
    }

    //sends our vertex data to the mesh
    mesh.setVertices(verts)

    //no need for depth...
//        Gdx.gl.glDepthMask(false);

    //enable blending, for alpha

    //number of vertices we need to render
    val vertexCount = idx / NUM_COMPONENTS

    //update the camera with our Y-up coordiantes
//        this.cam.setToOrtho(true, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    //start the shader before setting any uniforms
    shader.begin()

    //update the projection matrix so our triangles are rendered in 2D
    shader.setUniformMatrix("u_projTrans", camera.combined)

    //render the mesh
    mesh.render(shader, GL20.GL_TRIANGLES, 0, vertexCount)
    shader.end()

    //re-enable depth to reset states to their default
//        Gdx.gl.glDepthMask(true);

    //reset index to zero
    idx = 0
  }

  /**
   * @param cBit
   * Color bit
   * @param verts
   * Vertices to add
   */
  fun drawTriangle(cBit: Float, verts: FloatArray) {
    //we don't want to hit any index out of bounds exception...
    //so we need to flush the batch if we can't store any more verts
    if (idx == this.verts.size) {
      flush()
    }

    //now we push the vertex data into our array
    //we are assuming (0, 0) is lower left, and Y is up

    //bottom left vertex
    this.verts[idx++] = verts[0] //Position x
    this.verts[idx++] = verts[1] //Position y
    this.verts[idx++] = cBit //Color

    //top left vertex
    this.verts[idx++] = verts[2]
    this.verts[idx++] = verts[3]
    this.verts[idx++] = cBit

    //bottom right vertex
    this.verts[idx++] = verts[4]
    this.verts[idx++] = verts[5]
    this.verts[idx++] = cBit
  }

  override fun dispose() {
    mesh.dispose()
    shader.dispose()
  }

}
