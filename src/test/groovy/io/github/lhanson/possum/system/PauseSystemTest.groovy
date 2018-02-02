package io.github.lhanson.possum.system

import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.scene.Scene
import io.github.lhanson.possum.scene.SceneBuilder
import spock.lang.Specification

class PauseSystemTest extends Specification {
	PauseSystem pauseSystem
	Scene scene

	def setup() {
		pauseSystem = new PauseSystem()
		scene = SceneBuilder.createScene()
		pauseSystem.doInitScene(scene)
	}

	def "Pause events cause the game thread to wait"() {
		given:
			scene.activeInput = [MappedInput.PAUSE]
			Runnable game = { pauseSystem.doUpdate(scene, 1) }
			Thread gameThread = new Thread(game)

		when:
			gameThread.start()
			// Give the pause system time to process its input
			sleep 100

		then:
			gameThread.state == Thread.State.WAITING
	}

}
