package no.elg.hex.input

import no.elg.hex.input.EditMode.Companion.editModeSubclasses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author Elg
 */
internal class MapEditorInputTest {

  @Test
  internal fun `First EditMode has index of 0`() {
    assertEquals(0, editModeSubclasses.indexOf(MapEditorInput.editMode))
  }

  @Test
  internal fun `nextEditMode() is cyclic`() {
    for (edit in editModeSubclasses) {
      assertEquals(edit, MapEditorInput.editMode)
      MapEditorInput.nextEditMode()
    }

    assertEquals(editModeSubclasses[0], MapEditorInput.editMode)
  }
}
