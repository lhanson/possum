package io.github.lhanson.possum.scene

import io.github.lhanson.possum.input.InputAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
abstract class PossumSceneBuilder {
	/** The scene ID every game starts in by default */
	static final String START = 'start'

	@Autowired
	InputAdapter inputAdapter

	/**
	 * Indicates the scene which should be loaded before the next
	 * iteration of the main loop. Generally the same scene will
	 * run for a number of iterations and signal a state change
	 * by changing this value.
	 */
	String nextSceneId

	/**
	 * Return the active {@link Scene}.
	 *
	 * Components within a Scene will maintain state and determine when
	 * to transition to a different scene, which is indicated by setting
	 * {@code nextScene} to the ID of the next scene to be run.
	 *
	 * @return the Scene corresponding to the {@code nextSceneId}
	 */
	abstract Scene getNextScene()

}
