package no.elg.hex.platform.android

import android.annotation.TargetApi
import android.app.Activity
import android.app.GameManager
import android.app.GameState
import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import android.view.Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
import android.view.SurfaceControl
import android.view.View
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidGraphics
import com.badlogic.gdx.backends.android.surfaceview.GLSurfaceView20
import no.elg.hex.R
import no.elg.hex.platform.Platform

class AndroidPlatform(private val activity: Activity) : Platform {

  private val view: GLSurfaceView by lazy { (Gdx.graphics as AndroidGraphics).view as GLSurfaceView }

  override val version: String by lazy { activity.resources.getString(R.string.version) }

  private fun isAtLeast(version: Int): Boolean = Build.VERSION.SDK_INT >= version


  override val canLimitFps: Boolean = isAtLeast(Build.VERSION_CODES.R)

  @TargetApi(Build.VERSION_CODES.R)
  override fun setFps(fps: Int) {
    if (canLimitFps){
      Gdx.app.log("AndroidPlatform", "Limiting FPS to $fps")
      view.holder.surface.setFrameRate(fps.toFloat(), FRAME_RATE_COMPATIBILITY_DEFAULT)
      SurfaceControl.Transaction.CREATOR
    }else{
      Gdx.app.log("AndroidPlatform", "Cannot limit FPS")
    }
  }
}