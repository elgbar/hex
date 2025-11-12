package no.elg.hex.preview

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class PreviewModifier {
  @JsonEnumDefaultValue
  NOTHING,
  SURRENDER,
  WON,
  LOST,
  AI_DONE
}