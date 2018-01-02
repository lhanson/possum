package io.github.lhanson.possum.input

import java.awt.event.InputEvent
import java.awt.event.KeyEvent

/**
 * A reusable input context for games with 8-way keyboard movement.
 * Custom handlers can be passed in for specific raw input events.
 */
class EightWayInputContext implements InputContext {
	Map<Integer, Runnable> keyCodeHandlers

	/**
	 * @param keyCodeHandlers a map of Runnable handlers by keyCode to override defaults
	 */
	EightWayInputContext(Map<Integer, Runnable> keyCodeHandlers) {
		this.keyCodeHandlers = keyCodeHandlers
	}

	@Override
	MappedInput mapInput(InputEvent rawInput) {
		if (rawInput instanceof KeyEvent) {
			if (keyCodeHandlers[rawInput.keyCode]) {
				keyCodeHandlers[rawInput.keyCode].run()
			} else {
				switch (rawInput.keyCode) {
					case rawInput.VK_Y:
						return MappedInput.UP_LEFT
					case rawInput.VK_UP:
					case rawInput.VK_K:
						return MappedInput.UP
					case rawInput.VK_U:
						return MappedInput.UP_RIGHT
					case rawInput.VK_DOWN:
					case rawInput.VK_J:
						return MappedInput.DOWN
					case rawInput.VK_B:
						return MappedInput.DOWN_LEFT
					case rawInput.VK_LEFT:
					case rawInput.VK_H:
						return MappedInput.LEFT
					case rawInput.VK_N:
						return MappedInput.DOWN_RIGHT
					case rawInput.VK_RIGHT:
					case rawInput.VK_L:
						return MappedInput.RIGHT
					case rawInput.VK_PLUS:
					case rawInput.VK_ADD:
					case rawInput.VK_SHIFT | rawInput.VK_EQUALS:
						return MappedInput.INCREASE_DEBUG_PAUSE
					case rawInput.VK_MINUS:
						return MappedInput.DECREASE_DEBUG_PAUSE
					case rawInput.VK_D:
						return MappedInput.DEBUG
						break
					case rawInput.VK_P:
						return MappedInput.PAUSE
						break
				}
			}
		}
		null
	}

}
