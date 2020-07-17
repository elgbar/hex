package no.elg.hex.jackson.mixin

import com.fasterxml.jackson.annotation.JsonValue

/**
 * @author Elg
 */
class MaybeMixIn {

  @JsonValue
  lateinit var value: Any
}
