package no.elg.hex.util

import com.badlogic.gdx.math.Rectangle

/**
 * @author Elg
 */
operator fun Rectangle.component1(): Float = this.x
operator fun Rectangle.component2(): Float = this.y
operator fun Rectangle.component3(): Float = this.width
operator fun Rectangle.component4(): Float = this.height
