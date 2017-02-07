package io.github.lhanson.possum.scene

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.input.InputAdapter
import io.github.lhanson.possum.input.InputContext
import io.github.lhanson.possum.input.MappedInput

/**
 * A Scene encapsulates entities and input representing
 * a particular segment of a game.
 */
class Scene {
	InputAdapter inputAdapter

	/** Game entities active in this scene */
	List<GameEntity> entities = []
	/** Input contexts for this scene */
	List<InputContext> inputContexts =[]
	/** The input collected for this scene to process */
	Set<MappedInput> activeInput = []

	/**
	 * Collects raw input from the available {@link InputAdapter} and passes it to each
	 * {@link InputContext} within the current scene for mapping into
	 * platform-agnostic, high-level events to be fed into game logic.
	 */
	void processInput() {
		inputAdapter.collectInput()?.each { input ->
			MappedInput mapped = inputContexts.findResult { context ->
				context.mapInput(input)
			}
			if (mapped) {
				activeInput << mapped
			}
		}
	}
}
