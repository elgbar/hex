package no.elg.hex.platform.android

import android.content.res.Resources
import no.elg.hex.R
import no.elg.hex.platform.Platform

class AndroidPlatform(private val resources: Resources) : Platform {

  override val version: String by lazy { resources.getString(R.string.version) }
}