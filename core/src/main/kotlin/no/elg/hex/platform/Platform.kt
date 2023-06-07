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
  val defaultMSAA: Int

  val canControlAudio: Boolean

  fun platformInit() = Unit

  /**
   * Platform specific method to enable pause
   */
  fun pause() = Unit

  /**
   * Provide platform specific way of logging as trace, i.e., log level below debug.
   *
   * No check for whether trace logging is enable is expected to be done
   */
  fun trace(tag: String, exception: Throwable?, message: String)

  val type: PlatformType
}