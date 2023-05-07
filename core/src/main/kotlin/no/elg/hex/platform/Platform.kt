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
   * Whether toggling of vsync is supported on this platform
   */
  val canToggleVsync: Boolean

  val canSetMSAA: Boolean

  val canControlAudio: Boolean

  fun platformInit() = Unit

  /**
   * Platform specific method to enable pause
   */
  fun pause() = Unit

  val type: PlatformType
}