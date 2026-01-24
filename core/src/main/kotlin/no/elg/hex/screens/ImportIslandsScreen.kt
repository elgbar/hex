package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import ktx.async.skipFrame
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.util.playMoney
import no.elg.hex.util.safeUse
import kotlin.system.measureTimeMillis

class ImportIslandsScreen(private val jobs: List<Deferred<Pair<FastIslandMetadata, Island>?>>) : AbstractScreen() {

  private val startTime: Long = System.currentTimeMillis()

  private val layout by lazy { GlyphLayout() }

  private var maxRenderingJobs = jobs.size

  private var renderIslandTotalTime: Long = 0
  private var renderIslandCount = 0

  private val loaderJob = KtxAsync.launch {
    coroutineScope {
      for (importJob in jobs) {
        renderIslandTotalTime += measureTimeMillis {
          val await = importJob.await()
          if (await == null) {
            maxRenderingJobs--
            continue
          }
          val (metadata, island) = await

          Hex.assets.islandPreviews.updatePreviewFromIslandSync(metadata, island)
          skipFrame()
        }
        renderIslandCount++
      }
    }
  }

  private fun onDone() {
    Hex.assets.islandPreviews.sortIslands()
    Hex.screen = LevelSelectScreen()
    playMoney()
    Gdx.app.log("IS SPLASH", "Imported ${jobs.size} islands in ${System.currentTimeMillis() - startTime} ms")
  }

  private fun onWaiting() {
    batch.safeUse {
      val jobsLeft = maxRenderingJobs - renderIslandCount
      val renderTimeLeft = when (renderIslandCount) {
        0 -> "???.???"
        else -> {
          val msLeft = (renderIslandTotalTime / renderIslandCount) * jobsLeft
          "${"%7.3f".format(msLeft / 1000.0)} s"
        }
      }
      val progressbar = let {
        val progressSize: Int = ((Gdx.graphics.width * 0.75) / Hex.assets.regularFont.spaceXadvance).toInt()

        val percentDown: Int = ((jobsLeft.toFloat() / maxRenderingJobs) * progressSize).toInt()
        "|${"#".repeat(progressSize - percentDown)}${".".repeat(percentDown)}|"
      }
      val txt =
        """
          |Importing Islands 
          |
          |Islands imported $renderIslandCount / $maxRenderingJobs
          |
          |Actual time used    ${"%7.3f".format((System.currentTimeMillis() - startTime) / 1000.0)} s
          |Estimated time left $renderTimeLeft
          |
          |$progressbar
        """.trimMargin()

      layout.setText(
        Hex.assets.regularFont,
        txt,
        Color.WHITE,
        Gdx.graphics.width.toFloat(),
        Align.center,
        true
      )
      Hex.assets.regularFont.draw(batch, layout, 0f, (Gdx.graphics.height - layout.height) / 2)
      Gdx.graphics.requestRendering()
    }
  }

  override fun render(delta: Float) {
    if (loaderJob.isCompleted) {
      onDone()
    } else {
      onWaiting()
    }
  }
}