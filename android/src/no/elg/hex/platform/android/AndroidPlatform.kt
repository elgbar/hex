package no.elg.hex.platform.android

import android.app.Activity
import no.elg.hex.R
import no.elg.hex.platform.Platform

class AndroidPlatform(private val activity: Activity) : Platform {

  override val version: String by lazy { activity.resources.getString(R.string.version) }

  override val canLimitFps: Boolean = false

  override val canToggleVsync: Boolean = false

}