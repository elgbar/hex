package no.elg.hex.util

import com.badlogic.gdx.Gdx
import java.util.*

fun loadProperties(path: String): Properties = Properties().also {
  try {
    Gdx.files.internal(path).read().use { inputStream ->
      it.load(inputStream)
    }
  } catch (e: Exception) {
    Gdx.app.error("Assets", "Failed to load properties file $path: ${e.message}")
  }
}