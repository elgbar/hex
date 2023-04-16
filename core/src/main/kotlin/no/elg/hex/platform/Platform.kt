package no.elg.hex.platform

interface Platform {

  /**
   * Current version of Hex
   */
  val version: String

  /**
   * Whether the current platform is able to limit FPS
   */
  val canLimitFps: Boolean

  /**
   * Only supported when [canLimitFps] is `true`
   */
  fun setFps(fps: Int) = Unit

  /**
   * Whether toggling of vsync is supported on this platform
   */
  val canToggleVsync: Boolean

  val canSetMSAA: Boolean

  val canControlAudio: Boolean

  fun platformInit() = Unit
}