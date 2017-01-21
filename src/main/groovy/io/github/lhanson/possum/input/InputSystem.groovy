package io.github.lhanson.possum.input

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
/**
 * Collects input.
 *
 * The {@code InputSystem} is not like a generic {@link io.github.lhanson.possum.system.GameSystem}
 * in that it is handled separately in the main loop.
 */
@Component
class InputSystem {
	private Logger log = LoggerFactory.getLogger(this.class)
	@Autowired InputAdapter inputAdapter

	/**
	 * TODO
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
