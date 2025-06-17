package no.elg.hex.platform

interface Platform {

  /**
   * Current version of Hex
   */
  val version: String?

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

  /**
   * Set the text to the clipboard
   *
   * @param label User-visible label for the clip data, might be ignored by some platforms
   * @param data The actual data to be copied to the clipboard
   * @return `true` if a notification should be shown that the text was copied to the clipboard
   */
  fun writeToClipboard(label: String, data: Any): Boolean

  /**
   * Read a string from the clipboard
   * @return The text from the clipboard or `null` if there is no text in the clipboard
   */
  fun readStringFromClipboard(): String?
}