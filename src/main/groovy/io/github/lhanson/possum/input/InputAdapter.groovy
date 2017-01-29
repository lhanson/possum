package io.github.lhanson.possum.input

/**
 * Collects raw input from the underlying system.
 */
interface InputAdapter {
	/**
	 * Collects raw input from the underlying platform to be processed.
	 *
	 * @return list of collected {@link RawInput}
	 */
	List<RawInput> collectInput()
}
