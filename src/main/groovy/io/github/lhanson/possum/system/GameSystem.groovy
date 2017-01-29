package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.scene.Scene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A GameSystem operates on a entities having {@link GameComponent}s
 * which the particular system is aware of, thereby enabling specific behaviors.
 *
 * For example, a character movement system may process all {@link GameEntity}s
 * possessing Position, Velocity, and Collider {@link GameComponent}s to facilitate
 * movement.
 */
interface GameSystem {
	Logger log = LoggerFactory.getLogger(this.class)

	/**
	 * @return the name of this {@code GameSystem}
	 */
	String getName()

	/**
	 * For each main loop iteration, each system has the opportunity to
	 * perform some work, including processing entities it knows how to deal with.
	 *
	 * // TODO: Revise
	 * @param entities the game entities available for processing, never null
	 * @param elapsed the amount of time since the last game update
	 */
	void update(Scene scene, double elapsed)
}
