package io.github.lhanson.possum

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.system.GameStateSystem
import io.github.lhanson.possum.system.GameSystem
import io.github.lhanson.possum.system.InputSystem
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
	@Autowired GameStateSystem gameState
	@Autowired List<InputSystem> inputSystems
	@Autowired List<RenderingSystem> renderers
	@Autowired List<GameSystem> systems
	@Autowired List<GameEntity> entities
	Logger log = LoggerFactory.getLogger(Game)
	boolean initialized = false

	static void main(String[] args) {
		SpringApplication.run(Game, args)
	}

	void run() {
		// Main loop
		log.debug "Beginning main loop, initialized: $initialized"
		while(gameState.run) {
			log.debug "LOOP"
			inputSystems.each { it.processInput(entities) }
			systems.each { system ->
				log.debug "Processing ${system.name} system"
				system.update(entities)
			}
			renderers.each { it.render(entities) }
			Thread.sleep(500)
		}
		log.debug "Exiting"
	}

}
