package io.github.lhanson.possum

import io.github.lhanson.possum.scene.PossumSceneBuilder
import io.github.lhanson.possum.scene.Scene
import io.github.lhanson.possum.system.GameSystem
import io.github.lhanson.possum.rendering.RenderingSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MainLoop {
	@Autowired PossumSceneBuilder sceneBuilder
	@Autowired List<RenderingSystem> renderers
	@Autowired(required = false) List<GameSystem> systems
	Logger log = LoggerFactory.getLogger(MainLoop)

	//double MS_PER_UPDATE = 20
	//double lag = MS_PER_UPDATE // TODO: locking this into a 1-1 update/render ratio for now
	double elapsed = 0
	double previous = System.currentTimeMillis()

	void run() {
		Scene scene = sceneBuilder.getNextScene()
		while (scene) {
			scene.processInput()

			//while (lag >= MS_PER_UPDATE) {
				systems.each { it.update(scene, elapsed) }
				scene.activeInput.clear()
			//	lag -= MS_PER_UPDATE
			//}
			renderers.each { it.render(scene) }

			// My Cosmological Constant. Without it, things don't work (rendering gets
			// weird and flickery), and I don't yet know why.
			Thread.sleep(45)
			calculateElapsed()
			scene = sceneBuilder.getNextScene()
		}

		log.debug "Exiting"
		System.exit(0)
	}

	/*
	 * Called each iteration of the main loop, computes
	 * elapsed time taken by the previous iteration.
	 */
	double calculateElapsed() {
		double current = System.currentTimeMillis()
		elapsed = current - previous
		previous = current
		//lag += elapsed
	}

}
