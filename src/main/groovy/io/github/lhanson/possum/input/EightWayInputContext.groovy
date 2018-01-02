package io.github.lhanson.possum.input

import java.awt.event.InputEvent
import java.awt.event.KeyEvent

class EightWayInputContext implements InputContext {
	// TODO: how do we generalize this so that mappings return the proper MappedInput,
	// TODO: but we can also fire actions like "transition(START)"?
	@Override
	MappedInput mapInput(InputEvent rawInput) {
		if (rawInput instanceof KeyEvent) {
			switch (rawInput.keyCode) {
				case rawInput.VK_UP:
					return MappedInput.UP
				case rawInput.VK_DOWN:
					return MappedInput.DOWN
				case rawInput.VK_LEFT:
					return MappedInput.LEFT
				case rawInput.VK_RIGHT:
					return MappedInput.RIGHT
				case rawInput.VK_PLUS:
				case rawInput.VK_ADD:
				case rawInput.VK_SHIFT | rawInput.VK_EQUALS:
					return MappedInput.INCREASE_DEBUG_PAUSE
				case rawInput.VK_MINUS:
					return MappedInput.DECREASE_DEBUG_PAUSE
				case rawInput.VK_ESCAPE:
//					transition(START)
					break
				case rawInput.VK_D:
					return MappedInput.DEBUG
					break
				case rawInput.VK_P:
					return MappedInput.PAUSE
					break
			}
		}
		null
	}
}
