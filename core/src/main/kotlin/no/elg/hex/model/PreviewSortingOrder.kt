package no.elg.hex.model

import no.elg.hex.Settings
import no.elg.hex.island.Island
import no.elg.hex.util.toTitleCase
import java.util.Comparator.comparingInt
import java.util.function.ToIntFunction

enum class PreviewSortingOrder(private val rawComparator: Comparator<FastIslandMetadata>) {
  BY_ID(comparingInt(FastIslandMetadata::id)),
  BY_AUTHOR_ROUNDS(byRound(FastIslandMetadata::authorRoundsToBeat).thenComparing(BY_ID.rawComparator)),
  BY_USER_ROUNDS(byRound(FastIslandMetadata::userRoundsToBeat).thenComparing(BY_ID.rawComparator));

  private val reversedComparator by lazy { rawComparator.reversed() }

  override fun toString(): String = super.toString().replace('_', ' ').lowercase().toTitleCase()

  fun comparator(): Comparator<FastIslandMetadata> =
    if (Settings.reverseLevelSorting) {
      reversedComparator
    } else {
      rawComparator
    }
}

private fun byRound(roundsToBeat: FastIslandMetadata.() -> Int): Comparator<FastIslandMetadata> =
  comparingInt(
    ToIntFunction<FastIslandMetadata> { metadata ->
      val roundsToBeat1 = roundsToBeat(metadata)
      if (Island.NEVER_PLAYED == roundsToBeat1) {
        Int.MAX_VALUE / 2
      } else {
        roundsToBeat1
      }
    }
  )