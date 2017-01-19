package io.github.lhanson.possum.system

import io.github.lhanson.possum.entity.GameEntity

/**
 * Renders entities to whatever display system is in use.
 *
 * A {@code RenderingSystem} is not like a generic {@link GameSystem}
 * in that it is handled separately in the main loop.
 */
interface RenderingSystem {
	/**
	 * Renders the current game state
	 */
	void render(List<GameEntity> entities)
}
