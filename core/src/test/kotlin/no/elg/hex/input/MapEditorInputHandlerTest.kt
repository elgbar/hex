package no.elg.hex.input

import no.elg.hex.input.editor.OpaquenessEditor.Companion.OPAQUENESS_EDIT_SUBCLASSES
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author Elg
 */
internal class MapEditorInputHandlerTest {

  @Test
  internal fun `First EditMode has index of 0`() {
    assertEquals(0, OPAQUENESS_EDIT_SUBCLASSES.indexOf(MapEditorInputHandler.opaquenessEditor))
  }

  @Test
  internal fun `nextEditMode() is cyclic`() {
    for (edit in OPAQUENESS_EDIT_SUBCLASSES) {
      assertEquals(edit, MapEditorInputHandler.opaquenessEditor)
      MapEditorInputHandler.nextEditMode()
    }

    assertEquals(OPAQUENESS_EDIT_SUBCLASSES[0], MapEditorInputHandler.opaquenessEditor)
  }
}
