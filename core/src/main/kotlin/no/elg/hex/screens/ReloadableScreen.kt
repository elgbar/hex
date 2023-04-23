package no.elg.hex.screens

import com.badlogic.gdx.Screen

/**
 * Marks that this screen can be constructed by
 */
interface ReloadableScreen : Screen {

  /**
   * Recreate this screen after a reload
   */
  fun recreate(): AbstractScreen

}