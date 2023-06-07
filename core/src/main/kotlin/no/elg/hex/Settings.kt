package no.elg.hex

import com.badlogic.gdx.Gdx
import no.elg.hex.ai.Difficulty
import no.elg.hex.hexagon.Team
import no.elg.hex.hud.GLProfilerRenderer
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.util.delegate.PreferenceDelegate
import no.elg.hex.util.delegate.ResetSetting
import no.elg.hex.util.resetAllIslandProgress
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

@Suppress("unused")
object Settings {

  const val ENABLE_AUDIO_PATH = "enableAudio" // Settings::enableAudio.name
  var enableAudio by PreferenceDelegate(
    true,
    priority = 200,
    preferences = Hex.launchPreference,
    requireRestart = true,
    shouldHide = { !Hex.platform.canControlAudio }
  )

  const val VSYNC_PATH = "vsync" // Settings::vsync.name
  var vsync by PreferenceDelegate(
    true,
    priority = 200,
    preferences = Hex.launchPreference,
    shouldHide = { !Hex.platform.canToggleVsync },
    onChange = { _, _, new ->
      Gdx.graphics.setVSync(new)
      return@PreferenceDelegate new
    }
  )

  var volume by PreferenceDelegate(
    1f,
    priority = 210,
    shouldHide = { !Hex.platform.canControlAudio }
  ) { it < 0f || it > 1f }

  var confirmEndTurn by PreferenceDelegate(true, priority = 100)
  var confirmSurrender by PreferenceDelegate(true, priority = 100)
  var allowAIToSurrender by PreferenceDelegate(true, priority = 100_000, shouldHide = { !Hex.debug })

  var enableHoldToMarch by PreferenceDelegate(true, priority = 100)
  var enableStrengthHint by PreferenceDelegate(true, priority = 100)
  var enableStrengthBar by PreferenceDelegate(true, priority = 100)

  var showFps by PreferenceDelegate(false, priority = 109, shouldHide = { !Hex.debug })

  const val MSAA_SAMPLES_PATH = "MSAA" // Settings::MSAA.name
  var MSAA by PreferenceDelegate(4, Hex.launchPreference, true, shouldHide = { !Hex.platform.canSetMSAA }) { it !in 0..16 }

  var zoomSpeed by PreferenceDelegate(
    0.2f,
    priority = 100_000,
    shouldHide = { !Hex.debug }
  ) { it < 0.001f || it > 1f }

  var startTeam by PreferenceDelegate(
    Team.LEAF,
    runOnChangeOnInit = false,
    applyOnChangeOnSettingsHide = true,
    shouldHide = { !Hex.debug },
    priority = 100_000,
    onChange = { _, _, new ->
      gotoLevelSelect()
      return@PreferenceDelegate new
    }
  )

  var teamSunAI by PreferenceDelegate(
    Difficulty.HARD,
    runOnChangeOnInit = false,
    applyOnChangeOnSettingsHide = true,
    priority = 90,
    onChange = { _, _, new ->
      gotoLevelSelect()
      return@PreferenceDelegate new
    }
  )

  var teamLeafAI by PreferenceDelegate(
    Difficulty.PLAYER,
    runOnChangeOnInit = false,
    applyOnChangeOnSettingsHide = true,
    priority = 89,
    onChange = { _, _, new ->
      gotoLevelSelect()
      return@PreferenceDelegate new
    }
  )
  var teamForestAI by PreferenceDelegate(
    Difficulty.HARD,
    runOnChangeOnInit = false,
    applyOnChangeOnSettingsHide = true,
    priority = 90,
    onChange = { _, _, new ->
      gotoLevelSelect()
      return@PreferenceDelegate new
    }
  )

  var teamEarthAI by PreferenceDelegate(
    Difficulty.HARD,
    runOnChangeOnInit = false,
    applyOnChangeOnSettingsHide = true,
    priority = 90,
    onChange = { _, _, new ->
      gotoLevelSelect()
      return@PreferenceDelegate new
    }
  )

  var teamStoneAI by PreferenceDelegate(
    Difficulty.HARD,
    runOnChangeOnInit = false,
    applyOnChangeOnSettingsHide = true,
    priority = 90,
    onChange = { _, _, new ->
      gotoLevelSelect()
      return@PreferenceDelegate new
    }
  )

  var enableGLDebugging by PreferenceDelegate(
    false,
    priority = 100_000,
    shouldHide = { !Hex.debug },
    onChange = { _, old, new ->
      if (new != old) {
        if (new) {
          GLProfilerRenderer.enable()
        } else {
          GLProfilerRenderer.disable()
        }
      }

      return@PreferenceDelegate new
    }
  )

  var enableDebugHUD by PreferenceDelegate(true, priority = 100_000, shouldHide = { !Hex.debug && !Hex.args.mapEditor })
  var enableDebugFPSGraph by PreferenceDelegate(false, priority = 100_000, shouldHide = { !Hex.debug })

  val deleteAllProcess = ResetSetting("Are you use you want to delete all your progress?") {
    resetAllIslandProgress()
    MessagesRenderer.publishWarning("All progress have been deleted")
    Hex.screen = LevelSelectScreen()
  }

  @Suppress("UNCHECKED_CAST")
  val resetSettings = ResetSetting("Are you use you want to reset settings?") {
    Gdx.app.postRunnable {
      for (
      (property, loopDelegate) in Settings::class.declaredMemberProperties //
        .associateWith { it.also { it.isAccessible = true }.getDelegate(Settings) } //
        .filterValues { it is PreferenceDelegate<*> && it !is ResetSetting } //
      ) {
        // Nullable types are not allowed, this is ok cast
        (loopDelegate as PreferenceDelegate<Any>).setValue(Settings, property, loopDelegate.initialValue)
      }
    }

    MessagesRenderer.publishWarning("All settings have been reset")
    Hex.screen = LevelSelectScreen()
  }

  private var lastGotoCalled = 0L
  private fun gotoLevelSelect() {
    val frameId = Gdx.graphics.frameId
    if (lastGotoCalled != frameId) {
      lastGotoCalled = frameId
      // do next frame to fix cyclic problem
      Gdx.app.postRunnable { Hex.screen = LevelSelectScreen() }
    }
  }
}