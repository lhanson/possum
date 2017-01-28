package io.github.lhanson.possum.input

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Collects raw input and maps it to high-level events.
 *
 * The {@code InputSystem} is not like a generic {@link io.github.lhanson.possum.system.GameSystem}
 * in that it is handled separately in the main loop.
 */
@Component
class InputSystem {
	private Logger log = LoggerFactory.getLogger(this.class)
	@Autowired InputAdapter inputAdapter

	/**
	 * Collects raw input from the available {@link InputAdapter} and passes it to each
	 * provided {@link InputContext} for mapping into platform-agnostic, high-level events
	 * to be fed into game logic.
	 *
	 * @param the list of input contexts active in the current game mode
	 * @returns a list of platform-agnostic input events corresponding to user input
	 */
	List<MappedInput> processInput(List<InputContext> inputContexts) {
		List<MappedInput> mappedInput = []
		inputAdapter.collectInput()?.each { input ->
			MappedInput mapped = inputContexts.findResult { context ->
				context.mapInput(input)
			}
			if (mapped) {
				mappedInput << mapped
			}
		}
		return mappedInput
	}
}
