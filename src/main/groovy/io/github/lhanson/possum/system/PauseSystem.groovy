package io.github.lhanson.possum.system

import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.scene.Scene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * System which detects a {@link MappedInput#PAUSE} event and
 * causes the game thread to wait() until notified by the
 * {@link io.github.lhanson.possum.input.InputAdapter} (listening
 * on the AWT event thread) that the mapped PAUSE event has fired again.
 */
@Component
class PauseSystem extends GameSystem {
	private Logger log = LoggerFactory.getLogger(this.class)
	String name = 'PauseSystem'
	Scene currentScene
	Set<String> pausedScenes = [] as Set

	@Override
	void doUninitScene(Scene scene) {
		pausedScenes.remove(scene.id)
	}

	@Override
	void doUpdate(Scene scene, double elapsed) {
		currentScene = scene
		scene.activeInput.each { input ->
			switch (input) {
				case (MappedInput.PAUSE):
					if (paused(scene.id)) {
						unpause(scene.id)
					} else {
						pause(scene.id)
					}
					break
			}
		}
		while (paused(scene.id)) {
			synchronized (scene) {
				log.debug "Pausing scene"
				scene.wait()
				log.debug("Paused scene was woken, still paused: {}", paused(scene.id))
			}
		}
	}

	/** Returns whether the given scene is paused */
	boolean paused(String sceneId) {
		pausedScenes.contains(sceneId)
	}

	/** Pauses the given scene */
	void pause(String sceneId) {
		pausedScenes.add(sceneId)
	}

	/** Unpauses the given scene */
	void unpause(String sceneId) {
		pausedScenes.remove(sceneId)
	}

}
