package io.github.lhanson.possum.scene

import io.github.lhanson.possum.input.InputAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
abstract class PossumSceneBuilder {
	/** The scene ID every game starts in by default */
	static final String START = 'start'

	@Autowired InputAdapter inputAdapter

	/**
	 * Indicates the scene which should be loaded before the next
	 * iteration of the main loop. It will begin as the ID of *this*
	 * scene, i.e. the same scene will run each iteration, but
	 * is also used to indicate a scene transition.
	 */
	String nextSceneId

	/**
	 * For the provided {@code sceneId}, returns the active {@link Scene}.
	 * Components within a Scene will maintain state and determine when
	 * to transition to a different scene, which is indicated by setting
	 * {@code nextScene} to the ID of the next scene to be run.
	 * TODO: redo docs
	 * @param sceneId the ID of the currently active Scene
	 * @return the Scene corresponding to the given ID
	 */
	abstract Scene getNextScene()
//	abstract Scene getSceneForId(String sceneId)

}
