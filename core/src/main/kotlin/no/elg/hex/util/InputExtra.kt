package no.elg.hex.util

import com.badlogic.gdx.Gdx

fun Int.isKeyPressed() = Gdx.input.isKeyPressed(this)
fun Int.isKeyJustPressed() = Gdx.input.isKeyJustPressed(this)
fun Int.isButtonPressed() = Gdx.input.isButtonPressed(this)
fun Int.isButtonJustPressed() = Gdx.input.isButtonJustPressed(this)