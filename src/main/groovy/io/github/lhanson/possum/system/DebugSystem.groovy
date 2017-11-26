package io.github.lhanson.possum.system

import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.scene.Scene
import org.springframework.stereotype.Component

/**
 * System which detects a {@link MappedInput#DEBUG} event and
 * sets {@link Scene} state accordingly.
 */
@Component
class DebugSystem extends GameSystem {
	String name = 'debugSystem'

	@Override
	void doUpdate(Scene scene, double elapsed) {
		scene.activeInput.each { input ->
			switch (input) {
				case (MappedInput.DEBUG):
					scene.debug = !scene.debug
					log.debug "Toggled debug mode from ${!scene.debug} to ${scene.debug}"
					break
				case (MappedInput.INCREASE_DEBUG_PAUSE):
					scene.debugPauseMillis += 200
					log.debug "Increased debug pause to ${scene.debugPauseMillis}"
					break
				case (MappedInput.DECREASE_DEBUG_PAUSE):
					scene.debugPauseMillis -= 200
					log.debug "Decreased debug pause to ${scene.debugPauseMillis}"
					break
			}
		}
	}
}
