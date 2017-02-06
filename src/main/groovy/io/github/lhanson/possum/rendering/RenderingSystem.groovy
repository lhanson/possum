package io.github.lhanson.possum.rendering

import io.github.lhanson.possum.entity.GameEntity

/**
 * Renders entities to whatever display system is in use.
 *
 * A {@code RenderingSystem} is not like a generic
 * {@link io.github.lhanson.possum.system.GameSystem}
 * in that it is handled separately in the main loop.
 */
interface RenderingSystem {
	/**
	 * Renders the current game state
	 */
	void render(List<GameEntity> entities)

	/**
	 * @return the width of the current viewport
	 */
	int getViewportWidth()

	/**
	 * @return the width of the current viewport
	 */
	int getViewportHeight()
}
