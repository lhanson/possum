package io.github.lhanson.possum.input

/**
 * Interface representing a context-specific set of mappings
 * from raw input to higher-level {@link MappedInput}.
 */
interface InputContext {
	/**
	 * @param rawInput raw input events captured from the underlying system
	 * @return game-level events appropriate for the current situation
	 */
	MappedInput mapInput(RawInput rawInput)
}
