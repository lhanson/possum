package io.github.lhanson.possum

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.gameState.GameState
import io.github.lhanson.possum.system.GameSystem
import io.github.lhanson.possum.input.InputSystem
import io.github.lhanson.possum.system.RenderingSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext

@SpringBootApplication
class Game {
	@Autowired ConfigurableApplicationContext applicationContext
	@Autowired GameState gameState
	@Autowired List<InputSystem> inputSystems
	@Autowired List<RenderingSystem> renderers
	@Autowired(required = false) List<GameSystem> systems
	Logger log = LoggerFactory.getLogger(Game)

	static void main(String[] args) {
		SpringApplication.run(Game, args)
	}

	void run() {
		// The GameStateSystem determines what mode the game is in (main menu,
		// cut scene, game level, high score screen, etc.) and therefore
		// determines what entities are in play for the main loop at any
		// given time.
		List<GameEntity> entities
		while (entities = gameState.entitiesForCurrentState()) {
			inputSystems.each { it.processInput(entities) }
			systems.each { system ->
				log.debug "Processing ${system.name} system"
				system.update(entities)
			}
			renderers.each { it.render(entities) }
			Thread.sleep(500)
		}
		log.debug "Exiting"
		System.exit(0)
	}

}
