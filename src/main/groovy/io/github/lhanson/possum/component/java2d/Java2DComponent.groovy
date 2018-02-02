package io.github.lhanson.possum.component.java2d

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent

import java.awt.Graphics

/**
 * Represents a visual component renderable with the Java 2D API.
 */
abstract class Java2DComponent implements GameComponent {

	/** Render state to the given graphics object */
	abstract void draw(Graphics g, AreaComponent areaComponent)

}
