package io.github.lhanson.possum.scene

import io.github.lhanson.possum.input.InputAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
abstract class PossumSceneBuilder {
	static String START = 'start' // The scene ID every game starts in by default
	private String nextSceneId = START
	Map<String, Scene> scenesById

	@Autowired
	InputAdapter inputAdapter

	@PostConstruct
	def init() {
		initializeScenes()
		scenesById.values().each { it.inputAdapter = inputAdapter }
	}

	/**
	 * Called after construction and autowiring occurs, allows
	 * implementations to perform initial scene creation.
	 */
	abstract void initializeScenes()

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
	void transition(String nextSceneId) {
		this.nextSceneId = nextSceneId
	}
}
