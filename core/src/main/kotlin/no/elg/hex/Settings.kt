package no.elg.hex

import com.badlogic.gdx.Gdx
import no.elg.hex.ai.Difficulty
import no.elg.hex.hexagon.Team
import no.elg.hex.hud.GLProfilerRenderer
import no.elg.hex.model.PreviewSortingOrder
import no.elg.hex.platform.PlatformType
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
    preferences = Hex.launchPreference,
    requireRestart = true,
    priority = 200,
    afterChange = { _, _, new ->
      Hex.audioDisabled = !new
      Hex.music.toggleMute()
    },
    shouldHide = { !Hex.platform.canControlAudio }
  )

  const val VSYNC_PATH = "vsync" // Settings::vsync.name
  var vsync by PreferenceDelegate(
    true,
    preferences = Hex.launchPreference,
    priority = 200,
    afterChange = { _, _, new ->
      Gdx.graphics.setVSync(new)
    },
    shouldHide = { !Hex.platform.canToggleVsync }
  )

  var masterVolume by PreferenceDelegate(
    1f,
    priority = 210,
    afterChange = { _, _, _ ->
      Hex.music.updateMusicVolume()
    },
    shouldHide = { !Hex.platform.canControlAudio || Hex.audioDisabled }
  ) { it !in 0f..1f }

  var musicVolume by PreferenceDelegate(
    .5f,
    priority = 210,
    afterChange = { _, _, _ ->
      Hex.music.updateMusicVolume()
    },
    shouldHide = { !Hex.platform.canControlAudio || Hex.audioDisabled }
  ) { it !in 0f..1f }
  var musicPaused by PreferenceDelegate(false, priority = 211, afterChange = { _, _, _ -> Hex.music.toggleMute() })

  var confirmEndTurn by PreferenceDelegate(true, priority = 100)
  var confirmSurrender by PreferenceDelegate(true, priority = 100)
  var confirmRestartIsland by PreferenceDelegate(true, priority = 100, shouldHide = { Hex.mapEditor }) // does nothing when in map editor
  var loadAlreadyCompletedIslands by PreferenceDelegate(false, priority = 100_000, shouldHide = { !Hex.debug })
  var allowAIToSurrender by PreferenceDelegate(true, priority = 100_000, shouldHide = { !Hex.debug })

  var enableHoldToMarch by PreferenceDelegate(true, priority = 100)
  var enableStrengthHint by PreferenceDelegate(true, priority = 100)
  var enableStrengthBar by PreferenceDelegate(true, priority = 100)
  var enableActionHighlight by PreferenceDelegate(false, priority = 100)
  var enableDoubleTapToZoom by PreferenceDelegate(false, priority = 100)
  var showIslandId by PreferenceDelegate(false, priority = 110, shouldHide = { Hex.mapEditor || Hex.debug }) // always show island id in map editor/debug
  var showArtbId by PreferenceDelegate(false, priority = 111, shouldHide = { Hex.mapEditor || Hex.debug }) // always show island id in map editor/debug

  var enableStrengthHintEverywhere by PreferenceDelegate(false, priority = 220, shouldHide = { !Hex.debug && !Hex.mapEditor })
  var enableStrengthHintInPlayerTerritories by PreferenceDelegate(false, priority = 221, shouldHide = { !Hex.debug })

  var showFps by PreferenceDelegate(false, priority = 109, shouldHide = { !Hex.debug })

  const val MSAA_SAMPLES_PATH = "MSAA" // Settings::MSAA.name
  var msaa by PreferenceDelegate(Hex.platform.defaultMSAA, Hex.launchPreference, true, shouldHide = { !Hex.platform.canSetMSAA }) { it !in 0..16 }

  var zoomSpeed by PreferenceDelegate(
    0.2f,
    priority = 100_000,
    shouldHide = { !Hex.debug }
  ) { it < 0.001f || it > 1f }

  var startTeam by PreferenceDelegate(
    Team.LEAF,
    priority = 100_000,
    runAfterChangeOnInit = false,
    shouldHide = { !Hex.debug }
  )

  var teamSunAI by PreferenceDelegate(
    Difficulty.HARD,
    priority = 90,
    runAfterChangeOnInit = false
  )

  var teamLeafAI by PreferenceDelegate(
    Difficulty.PLAYER,
    priority = 89,
    runAfterChangeOnInit = false
  )
  var teamForestAI by PreferenceDelegate(
    Difficulty.HARD,
    priority = 90,
    runAfterChangeOnInit = false
  )

  var teamEarthAI by PreferenceDelegate(
    Difficulty.HARD,
    priority = 90,
    runAfterChangeOnInit = false
  )

  var teamStoneAI by PreferenceDelegate(
    Difficulty.HARD,
    priority = 90,
    runAfterChangeOnInit = false
  )

  var enableGLDebugging by PreferenceDelegate(
    false,
    priority = 100_000,
    afterChange = { _, old, new ->
      if (new != old) {
        if (new) {
          GLProfilerRenderer.enable()
        } else {
          GLProfilerRenderer.disable()
        }
      }
    },
    shouldHide = { !Hex.debug }
  )

  var enableDebugHUD by PreferenceDelegate(true, priority = 100_000, shouldHide = { !Hex.debug && !Hex.mapEditor })
  var enableDebugFPSGraph by PreferenceDelegate(false, priority = 100_000, shouldHide = { !Hex.debug })

  var debugAIAction by PreferenceDelegate(false, priority = 200_100, shouldHide = { !Hex.debug })
  var debugAIActionDelayMillis by PreferenceDelegate(2000L, priority = 200_101, shouldHide = { !Hex.debug })
  var debugAICastlePlacement by PreferenceDelegate(false, priority = 200_102, shouldHide = { !Hex.debug && !Hex.mapEditor })

  var compressionPreset by PreferenceDelegate(6, priority = 100_000_000, shouldHide = { !Hex.debug }, invalidate = { it !in 0..9 })
  var compressExport by PreferenceDelegate(true, priority = 100_000_001, shouldHide = { !Hex.debug })

  var levelSorter by PreferenceDelegate(
    PreviewSortingOrder.BY_AUTHOR_ROUNDS,
    priority = 91,
    runAfterChangeOnInit = false,
    afterChange = { _, _, _ ->
      Hex.assets.islandPreviews.sortIslands()
    }
  )

  var reverseLevelSorting by PreferenceDelegate(
    false,
    priority = 92,
    runAfterChangeOnInit = false,
    afterChange = { _, _, _ ->
      Hex.assets.islandPreviews.sortIslands()
    }
  )

  var smoothScrolling by PreferenceDelegate(
    when (Hex.platform.type) {
      PlatformType.DESKTOP -> true
      PlatformType.MOBILE -> false
    },
    priority = 100,
    requireRestart = true
  )

  val deleteAllProgress = ResetSetting("Are you sure you want to delete all your progress?") {
    resetAllIslandProgress()
  }

  val resetSettings = ResetSetting("Are you sure you want reset all settings?") {
    Gdx.app.postRunnable {
      for (
      (property, loopDelegate) in Settings::class.declaredMemberProperties
        .associateWith { it.also { it.isAccessible = true }.getDelegate(Settings) }
        .filterValues { it is PreferenceDelegate<*> }
      ) {
        // Nullable types are not allowed, this is ok cast
        @Suppress("UNCHECKED_CAST")
        (loopDelegate as PreferenceDelegate<Any>).setValue(Settings, property, loopDelegate.initialValue)
      }
    }
  }
}