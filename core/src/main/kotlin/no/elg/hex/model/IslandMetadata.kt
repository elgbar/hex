package no.elg.hex.model

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.island.Island

data class IslandMetadata(
  val id: Int,
  val island: Island,
  val preview: Texture
) : Disposable {

  override fun dispose() {
    preview.dispose()
  }
}