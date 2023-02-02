package no.elg.hex.ai

import no.elg.hex.ai.NotAsRandomAI.Companion.PIECE_MAINTAIN_CONTRACT_LENGTH
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class NotAsRandomAITest {

  @Test
  fun shouldCreateWorks() {
    assertFalse { NotAsRandomAI.isEconomicalToCreatePiece(0, -1) }
    assertTrue { NotAsRandomAI.isEconomicalToCreatePiece(0, 1) }
    assertTrue { NotAsRandomAI.isEconomicalToCreatePiece(0, 0) }
    assertTrue { NotAsRandomAI.isEconomicalToCreatePiece(PIECE_MAINTAIN_CONTRACT_LENGTH, -1) }
    assertFalse { NotAsRandomAI.isEconomicalToCreatePiece(PIECE_MAINTAIN_CONTRACT_LENGTH, -2) }
  }
}