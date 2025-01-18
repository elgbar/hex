package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import ktx.async.skipFrame
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.util.playMoney
import no.elg.hex.util.safeUse

class ImportIslandsScreen(private val jobs: List<Deferred<Pair<FastIslandMetadata, Island>?>>) : AbstractScreen() {

  private val startTime: Long = System.currentTimeMillis()

  private val layout by lazy { GlyphLayout() }

  private var renderingsDone = 0
  private var maxRenderingJobs = jobs.size
  private val loaderJob = KtxAsync.launch(Hex.asyncThread) {
    for (job in jobs) {
      val await = job.await()
      if (await == null) {
        maxRenderingJobs--
        continue
      }
      val (metadata, island) = await
      Hex.assets.islandPreviews.updatePreviewFromIslandSync(metadata, island)
      renderingsDone++
      skipFrame()
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
      val txt =
        """
          |Importing Islands
          |
          | ${if (renderingsDone > 0) "Islands rendered $renderingsDone / $maxRenderingJobs" else "Islands loaded ${jobs.count { it.isCompleted }} / ${jobs.size}"}
          |
          |${System.currentTimeMillis() - startTime} ms
        """.trimMargin()

      layout.setText(
        Hex.assets.regularFont,
        txt,
        Color.WHITE,
        Gdx.graphics.width.toFloat(),
        Align.center,
        true
      )
      Hex.assets.regularFont.draw(batch, layout, 0f, Gdx.graphics.height.toFloat() / 2)
      Gdx.graphics.requestRendering()
    }
  }

  override fun render(delta: Float) {
    if (jobs.all { it.isCompleted } && loaderJob.isCompleted) {
      onDone()
    } else {
      onWaiting()
    }
  }
}