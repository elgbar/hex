package no.elg.hex.model

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.island.Island

data class IslandMetadata(val id: Int, val island: Island, val preview: Texture) : Disposable, Comparable<IslandMetadata> {

  override fun compareTo(other: IslandMetadata): Int {
    return if (island.authorRoundsToBeat <= Island.UNKNOWN_ROUNDS_TO_BEAT) {
      id.compareTo(other.id)
    } else {
      island.authorRoundsToBeat.compareTo(other.island.authorRoundsToBeat)
    }
  }

  override fun dispose() {
    preview.dispose()
  }
}