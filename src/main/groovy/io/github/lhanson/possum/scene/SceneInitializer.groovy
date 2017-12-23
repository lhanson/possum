package io.github.lhanson.possum.scene

import io.github.lhanson.possum.entity.GameEntity

/**
 * It initializes scenes, what do you think it does?
 */
interface SceneInitializer {
	/**
	 * Initialize the given scene before its first frame is run. This
	 * is intended to be an idempotent initialization which can be run
	 * multiple times to reset the scene to its initial state if desired.
	 *
	 * @return the list of game entities in the scene
	 */
	List<GameEntity> initScene()
}