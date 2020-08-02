package no.elg.hex.island

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Team
import org.hexworks.mixite.core.api.Hexagon

/**
 * @author Elg
 */
data class Hand(val team: Team, val piece: Piece, val originalHex: Hexagon<HexagonData>) {}

