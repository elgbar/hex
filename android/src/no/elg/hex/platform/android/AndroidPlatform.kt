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
import android.view.WindowManager
import no.elg.hex.Hex
import no.elg.hex.R
import no.elg.hex.platform.Platform
import no.elg.hex.platform.PlatformType
import no.elg.hex.util.compressB85
import no.elg.hex.util.decompressB85


class AndroidPlatform(private val activity: Activity) : Platform {

  override val version: String by lazy { activity.resources.getString(R.string.version) }

  override val canLimitFps: Boolean = false
  override val canToggleVsync: Boolean = false

  override val canSetMSAA: Boolean = false
  override val defaultMSAA: Int = 4
  override val canControlAudio: Boolean = false

  override fun platformInit() {
    if (VERSION.SDK_INT >= VERSION_CODES.R) {
      activity.window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
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

  override val type: PlatformType = PlatformType.MOBILE

  override fun writeToClipboard(label: String, data: Any): Boolean {
    val clipboard: ClipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val writeValueAsString = Hex.mapper.writeValueAsString(data)
    clipboard.setPrimaryClip(ClipData.newPlainText(label, compressB85(writeValueAsString)))
    return VERSION.SDK_INT <= VERSION_CODES.S_V2
  }

  override fun readFromClipboard(): String? {
    val clipboard: ClipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip?.let { clipData ->
      if(clipData.itemCount == 0) return null
      val raw = clipData.getItemAt(0).coerceToText(null).toString()
      decompressB85(raw)
    }
  }
}