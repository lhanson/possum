package io.github.lhanson.possum.input

import io.github.lhanson.possum.entity.GameEntity

/**
 * Collects input.
 *
 * An {@code InputSystem} is not like a generic {@link io.github.lhanson.possum.system.GameSystem}
 * in that it is handled separately in the main loop.
 */
interface InputSystem {
	/**
	 * @param entities game entities available for responding to input
	 */
	void processInput(List<GameEntity> entities)
}
