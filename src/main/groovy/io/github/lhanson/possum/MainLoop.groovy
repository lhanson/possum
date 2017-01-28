package io.github.lhanson.possum

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.gameState.GameState
import io.github.lhanson.possum.input.InputSystem
import io.github.lhanson.possum.system.GameSystem
import io.github.lhanson.possum.system.RenderingSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

import static io.github.lhanson.possum.gameState.Mode.EXIT

@Component
class MainLoop {
	@Autowired ConfigurableApplicationContext applicationContext
	@Autowired GameState gameState
	@Autowired List<RenderingSystem> renderers
	@Autowired InputSystem inputSystem
	@Autowired(required = false) List<GameSystem> systems
	Logger log = LoggerFactory.getLogger(MainLoop)

	//double MS_PER_UPDATE = 20
	//double lag = MS_PER_UPDATE // TODO: locking this into a 1-1 update/render ratio for now
	double elapsed = 0
	double previous = System.currentTimeMillis()

	void run() {
		List<GameEntity> entities
		while (gameState.currentMode != EXIT) {
			entities = gameState.getActiveEntities()
			if (entities == null) {
				break
			}

			gameState.collectInput(inputSystem)

			//while (lag >= MS_PER_UPDATE) {
				systems.each { it.update(entities, elapsed) }
				gameState.activeInput.clear()
			//	lag -= MS_PER_UPDATE
			//}
			renderers.each { it.render(entities) }

			// My Cosmological Constant. Without it, things don't work (rendering gets
			// weird and flickery), and I don't yet know why.
			Thread.sleep(45)
			calculateElapsed()
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
