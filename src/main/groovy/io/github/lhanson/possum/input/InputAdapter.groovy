package io.github.lhanson.possum.input

import java.awt.event.InputEvent

/**
 * Collects raw input from the underlying system.
 */
interface InputAdapter {
	/**
	 * Collects raw input from the underlying platform to be processed.
	 *
	 * @return list of collected {@link InputEvent}s
	 */
	List<InputEvent> collectInput()
}
