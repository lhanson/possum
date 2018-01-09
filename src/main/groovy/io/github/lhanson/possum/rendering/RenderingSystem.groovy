package io.github.lhanson.possum.rendering

import io.github.lhanson.possum.component.VectorComponent
import io.github.lhanson.possum.scene.Scene

/**
 * Renders entities to whatever display system is in use.
 *
 * A {@code RenderingSystem} is not like a generic
 * {@link io.github.lhanson.possum.system.GameSystem}
 * in that it is handled separately in the main loop.
 */
interface RenderingSystem {
	/**
	 * Renders the current scene
	 */
	void render(Scene scene)

	/**
	 * @return the width of the current viewport
	 */
	int getViewportWidth()

	/**
	 * @return the width of the current viewport
	 */
	int getViewportHeight()

	/**
	 * Centers the viewport around the given coordinates
	 * @param position the specified coordinates
	 */
	void centerViewport(VectorComponent position)

	/**
	 * Allows renderers to do scene-specific initialization before
	 * a scene transition.
	 *
	 * @param scene the scene being transitioned to
	 */
	void initScene(Scene scene)

	/**
	 * Allows renderers to do scene-specific cleanup during
	 * a scene transition.
	 *
	 * @param scene the scene being transitioned from
	 */
	void uninitScene(Scene scene)
}
