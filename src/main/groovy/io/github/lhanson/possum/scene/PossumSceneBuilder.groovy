package io.github.lhanson.possum.scene

import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.input.InputAdapter
import io.github.lhanson.possum.rendering.RenderingSystem
import io.github.lhanson.possum.system.GameSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class PossumSceneBuilder {
	static String START = 'start' // The scene ID every game starts in by default
	Logger log = LoggerFactory.getLogger(this.class)
	String nextSceneId = START
	Scene currentScene
	Map<String, Scene> scenesById = [:]
	@Autowired List<RenderingSystem> renderers
	@Autowired(required = false) List<GameSystem> systems
	@Autowired EventBroker eventBroker

	@Autowired
	InputAdapter inputAdapter

	/**
	 * Register a new {@code Scene} with the game
	 * @param scene the scene to register
	 */
	void addScene(Scene scene) {
		// We do a bit of manual wiring of dependencies
		// here since Scene isn't a Spring Bean.
		scene.inputAdapter = inputAdapter
		scene.eventBroker = eventBroker
		scenesById[scene.id] = scene
	}

	/**
	 * Return the active {@link Scene}.
	 *
	 * Components within a Scene will maintain state and determine when
	 * to transition to a different scene, which is indicated by setting
	 * {@code nextScene} to the ID of the next scene to be run.
	 *
	 * @return the Scene corresponding to the {@code nextSceneId}
	 */
	Scene getNextScene() {
		// This scene will loop until any of its components specify otherwise
		Scene nextScene = scenesById[nextSceneId]
		if (nextScene && nextScene != currentScene) {
			log.info "Scene change detected from {} to {}", currentScene?.id, nextScene?.id
			currentScene?.uninit()
			if (!nextScene.initialized) {
				nextScene.init()
			}
			systems.each { it.initScene(nextScene) }
			renderers.each { it.initScene(nextScene) }
			currentScene = nextScene
		}
		return nextScene
	}

	/**
	 * Indicates that the next iteration of the main loop should run
	 * the specified scene.
	 *
	 * @param nextSceneId the scene to run next, or null if the program should exit
	 */
	def transition = { String nextSceneId ->
		this.nextSceneId = nextSceneId
	}

}
