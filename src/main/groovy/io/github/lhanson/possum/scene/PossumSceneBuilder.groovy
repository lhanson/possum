package io.github.lhanson.possum.scene

import io.github.lhanson.possum.input.InputAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
abstract class PossumSceneBuilder {
	static String START = 'start' // The scene ID every game starts in by default
	String nextSceneId = START
	Map<String, Scene> scenesById = [:]

	@Autowired
	InputAdapter inputAdapter

	/**
	 * Register a new {@code Scene} with the game
	 * @param scene the scene to register
	 */
	void addScene(Scene scene) {
		scene.inputAdapter = inputAdapter
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
		scenesById[nextSceneId]
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
