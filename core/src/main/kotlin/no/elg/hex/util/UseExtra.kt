package no.elg.hex.util

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Automatically calls [Batch.begin] and [Batch.end]. If the batch is already drawing, it is ended before the action.
 * @param camera Uses the [Camera.combined] as the projection matrix.
 * @param action inlined. Executed after [Batch.begin] and before [Batch.end].
 */
inline fun <B : Batch> B.safeUse(camera: Camera, action: (B) -> Unit) = safeUse(camera.combined, action)

/**
 * Automatically calls [Batch.begin] and [Batch.end]. If the batch is already drawing, it is ended before the action.
 * @param projectionMatrix A projection matrix to set on the batch before [Batch.begin]. If null, the batch's matrix
 * remains unchanged.
 * @param action inlined. Executed after [Batch.begin] and before [Batch.end].
 */
inline fun <B : Batch> B.safeUse(projectionMatrix: Matrix4? = null, action: (B) -> Unit) {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  if (projectionMatrix != null) {
    this.projectionMatrix = projectionMatrix
  }
  if (!isDrawing) {
    begin()
  }
  try {
    action(this)
  } finally {
    if (isDrawing) {
      end()
    }
  }
}

/**
 * Automatically calls [GLFrameBuffer.begin] and [GLFrameBuffer.end]. Will always call [GLFrameBuffer.end] even if an exception is thrown.
 *
 * @param action inlined. Executed after [GLFrameBuffer.begin] and before [GLFrameBuffer.end].
 */
inline fun <B : GLFrameBuffer<*>> B.safeUse(action: (B) -> Unit) {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  begin()
  try {
    action(this)
  } finally {
    end()
  }
}

/**
 * Automatically calls [ShapeRenderer.begin] and [ShapeRenderer.end].
 * @param type specified shape type used to draw the shapes in the [action] block. Can be changed during the rendering with [ShapeRenderer.set].
 * @param camera Uses the [Camera.combined] as the projection matrix.
 * @param action inlined. Executed after [ShapeRenderer.begin] and before [ShapeRenderer.end].
 */
inline fun <SR : ShapeRenderer> SR.safeUse(type: ShapeRenderer.ShapeType, camera: Camera, action: (SR) -> Unit) = safeUse(type, camera.combined, action)

/**
 * Automatically calls [ShapeRenderer.begin] and [ShapeRenderer.end].
 * @param type specified shape type used to draw the shapes in the [action] block. Can be changed during the rendering
 * with [ShapeRenderer.set].
 * @param projectionMatrix A projection matrix to set on the ShapeRenderer before [ShapeRenderer.begin]. If null, the ShapeRenderer's matrix
 * remains unchanged.
 * @param action inlined. Executed after [ShapeRenderer.begin] and before [ShapeRenderer.end].
 */
inline fun <SR : ShapeRenderer> SR.safeUse(type: ShapeRenderer.ShapeType, projectionMatrix: Matrix4? = null, action: (SR) -> Unit) {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  if (projectionMatrix != null) {
    this.projectionMatrix = projectionMatrix
  }
  if (isDrawing) {
    end()
  }
  begin(type)
  try {
    action(this)
  } finally {
    end()
  }
}

fun <D : Disposable, R> D.useDispose(block: (D) -> R): R =
  try {
    block(this)
  } finally {
    dispose()
  }