package io.github.lhanson.possum.system

import io.github.lhanson.possum.entity.GameEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A GameSystem operates on a group of related {@link io.github.lhanson.possum.component.GameComponent}s to enable behavior.
 *
 * For example, a character movement system may process all {@link GameEntity}s
 * possessing Position, Velocity, and Collider {@GameComponent}s to facilitate
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
	 * @param entities the game entities available for processing
	 */
	void update(List<GameEntity> entities)
}
