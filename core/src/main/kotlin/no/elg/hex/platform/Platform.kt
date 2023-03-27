package no.elg.hex.platform

interface Platform {

  val version: String

  /**
   * Whether the current platform is able to limit FPS
   */
  val canLimitFps: Boolean

  /**
   * Only supported when [canLimitFps] is `true`
   */
  fun setFps(fps: Int)

  fun platformInit() = Unit
}