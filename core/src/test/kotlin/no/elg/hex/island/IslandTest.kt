package no.elg.hex.island

import com.badlogic.gdx.files.FileHandle
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** @author Elg */
internal class IslandTest {

  private fun getIsFile(filename: String): FileHandle = FileHandle(File("./src/test/resources/$filename"))

  @Test
  fun `calculateBestCapitalPlacement() All same color should return center`() {
    val loaded = Island.deserialize(getIsFile("island-all-sun-hexagonal-radius-3.is"))
    assertTrue(loaded.validate())
  }
}