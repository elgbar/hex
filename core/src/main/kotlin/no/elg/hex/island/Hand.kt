package no.elg.hex.island

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.hexagon.CASTLE_PRICE
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Piece
import no.elg.hex.util.trace

/** @author Elg */
data class Hand(
  val territory: Territory,
  val piece: Piece,
  /**
   * If the held piece should be refunded/returned when disposing.
   * This is useful when modifying the held piece without duplicating money/pieces
   */
  var restore: RestoreAction = DefaultRestoreAction
) : Disposable {

  init {
    require(piece !is LivingPiece || !piece.moved) {
      "Holding a living piece that has already moved"
    }
    require(piece != Empty) { "Cannot hold empty piece" }
  }

  /**
   * If this is the current held hand and has not been disposed
   */
  var currentHand = true
    private set

  override fun dispose() {
    require(currentHand) { "Hand already disposed " }
    currentHand = false
    Gdx.app.trace("HAND") { "Disposing hand $this, refund? ${restore::class.simpleName}" }
    restore.restore(this)
  }

  override fun toString(): String =
    "piece: ${piece::class.simpleName}, territory: $territory, restore: $restore"

  companion object {
    sealed interface RestoreAction {
      fun restore(hand: Hand)
      val serializedName: String?

      companion object {
        private val allInstances: List<RestoreAction> = RestoreAction::class.sealedSubclasses
          .map {
            it.objectInstance ?: error("All instances of RestoreAction must be an object, $it is not an object")
          }

        fun fromString(actionName: String?): RestoreAction =
          allInstances.find { it.serializedName == actionName } ?: DefaultRestoreAction

        fun toString(action: RestoreAction?): String? = action?.serializedName
      }
    }

    data object NoRestore : RestoreAction {
      override fun restore(hand: Hand) = Unit
      override fun toString(): String = "No restore"
      override val serializedName: String = "no"
    }

    data object RefundCastleSwapAction : RestoreAction {
      override fun restore(hand: Hand) {
        hand.territory.capital.balance += CASTLE_PRICE
        DefaultRestoreAction.restore(hand)
      }

      override fun toString(): String = "Castle Swap Restore"

      override val serializedName: String = "castle swap"
    }

    data object DefaultRestoreAction : RestoreAction {
      override fun restore(hand: Hand) {
        with(hand) {
          if (piece.data === HexagonData.EDGE_DATA) {
            hand.territory.capital.balance += hand.piece.price
          } else {
            val placed = piece.data.setPiece(piece::class)
            require(placed) { "Failed to place old hand piece back to where it was" }
            // restore the piece to its last placed location
            val newPiece = piece.data.piece
            if (newPiece is LivingPiece) {
              newPiece.moved = false
            }
          }
        }
      }

      override fun toString(): String = "Restore"
      override val serializedName: String? = null
    }
  }
}