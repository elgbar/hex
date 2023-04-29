package no.elg.hex.util

import com.badlogic.gdx.assets.AssetManager

inline fun <reified T> AssetManager.isLoaded(fileName: String): Boolean = isLoaded(fileName, T::class.java)

inline fun <reified T> AssetManager.fetch(fileName: String): T = get(fileName, T::class.java)