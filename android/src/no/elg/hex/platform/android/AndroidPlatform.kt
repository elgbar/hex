package no.elg.hex.platform.android

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import no.elg.hex.R
import no.elg.hex.platform.Platform
import no.elg.hex.platform.PlatformType


class AndroidPlatform(private val activity: Activity) : Platform {

  override val version: String by lazy { activity.resources.getString(R.string.version) }

  override val canLimitFps: Boolean = false
  override val canToggleVsync: Boolean = false

  override val canSetMSAA: Boolean = false
  override val defaultMSAA: Int = 4
  override val canControlAudio: Boolean = false

  override fun platformInit() {
    val window = activity.window
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    // Add a listener to update the behavior of the toggle fullscreen button when
    // the system bars are hidden or revealed.
    ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
      // You can hide the caption bar even when the other system bars are visible.
      // To account for this, explicitly check the visibility of navigationBars()
      // and statusBars() rather than checking the visibility of systemBars().
      if (windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars()) || windowInsets.isVisible(WindowInsetsCompat.Type.statusBars())) {
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
      }
      ViewCompat.onApplyWindowInsets(view, windowInsets)
    }
  }

  override fun pause() {
    Handler(Looper.getMainLooper()).post {
      @Suppress("DEPRECATION")
      activity.onBackPressed()
    }
  }

  override fun trace(tag: String, exception: Throwable?, message: String) {
    Log.v(tag, message, exception)
  }

  override val vsync: Boolean?
    get() = null

  override val type: PlatformType = PlatformType.MOBILE

  override fun writeToClipboard(label: String, data: String): Boolean {
    val clipboard: ClipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    clipboard.setPrimaryClip(ClipData.newPlainText(label, data))
    return VERSION.SDK_INT <= VERSION_CODES.S_V2
  }

  override fun readStringFromClipboard(): String? {
    val clipboard: ClipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip?.let { clipData ->
      if (clipData.itemCount == 0) return null
      clipData.getItemAt(0).coerceToText(null).toString()
    }
  }
}