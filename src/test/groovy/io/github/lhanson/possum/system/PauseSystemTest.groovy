package io.github.lhanson.possum.system

import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.scene.Scene
import spock.lang.Specification

class PauseSystemTest extends Specification {

	def "Pause events cause the game thread to wait"() {
		given:
			PauseSystem pauseSystem = new PauseSystem()
			Scene scene = new Scene('test-scene')
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
