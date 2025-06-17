package no.elg.hex.model

import no.elg.hex.Settings
import no.elg.hex.island.Island
import no.elg.hex.preview.PreviewModifier
import java.util.Comparator.comparingInt
import java.util.function.ToIntFunction

enum class PreviewSortingOrder(private val rawComparator: Comparator<FastIslandMetadata>) {
  /**
   * Sort islands by when they were created, i.e., ID
   */
  BY_ID(comparingInt(FastIslandMetadata::id)),

  /**
   * Sort islands by lowest amount of rounds known to beat it
   */
  BY_AUTHOR_ROUNDS(byRound(FastIslandMetadata::authorRoundsToBeat).thenComparing(BY_ID.rawComparator)),

  /**
   * Sort the islands by how many rounds the user used to beat it
   */
  BY_USER_ROUNDS(byRound(FastIslandMetadata::userRoundsToBeat).thenComparing(BY_ID.rawComparator)),

  /**
   * Sort islands by whether the user have tried to beat them
   */
  NOT_WON(byModifier().thenComparing(byWon()).thenComparing(BY_AUTHOR_ROUNDS.rawComparator)),

  /**
   * Sort islands by whether the user have tried to beat them
   */
  NOT_TRIED(byHaveTried().thenComparing(byWon()).thenComparing(BY_AUTHOR_ROUNDS.rawComparator))
  ;

  private val reversedComparator by lazy { rawComparator.reversed() }

  override fun toString(): String = super.toString().replace('_', ' ') // .lowercase().toTitleCase()

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

private fun byWon(): Comparator<FastIslandMetadata> =
  comparingInt(
    ToIntFunction<FastIslandMetadata> { metadata ->
      if (Island.NEVER_PLAYED == metadata.userRoundsToBeat) {
        0
      } else {
        Int.MAX_VALUE
      }
    }
  )

private fun byHaveTried(): Comparator<FastIslandMetadata> =
  comparingInt(
    ToIntFunction<FastIslandMetadata> { metadata ->
      if (metadata.modifier == PreviewModifier.NOTHING) {
        0
      } else {
        Int.MAX_VALUE
      }
    }
  )

private fun byModifier(): Comparator<FastIslandMetadata> =
  comparingInt(
    ToIntFunction<FastIslandMetadata> { metadata ->
      when (metadata.modifier) {
        PreviewModifier.SURRENDER -> 1
        PreviewModifier.LOST -> 2
        PreviewModifier.AI_DONE -> 3
        PreviewModifier.NOTHING -> 4
        PreviewModifier.WON -> 10
      }
    }
  )