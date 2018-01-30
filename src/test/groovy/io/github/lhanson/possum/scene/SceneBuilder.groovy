package io.github.lhanson.possum.scene

import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.rendering.Viewport

/** Convenience class for setting up a scene to use in tests */
class SceneBuilder {
	static int sceneId = 1

	/**
	 * @param initializer the scene initializer used to populate entities, if desired
	 * @return a new scene, initialized and ready to go
	 */
	static Scene createScene(SceneInitializer initializer) {
		Scene scene = new Scene("scene ${sceneId++}")
		scene.sceneInitializer = initializer
		scene.eventBroker = new EventBroker()
		scene.viewport = new Viewport()
		scene.init()
		return scene
	}

}
