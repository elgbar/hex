package no.elg.hex.jackson.mixin

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * @author Elg
 */
@JsonIgnoreProperties("hexagonHeight", "hexagonHeight", "innerRadius")
interface GridDataMixIn {
}
