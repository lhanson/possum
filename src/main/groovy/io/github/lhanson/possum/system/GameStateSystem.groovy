package io.github.lhanson.possum.system

import io.github.lhanson.possum.entity.GameEntity
import org.springframework.stereotype.Component

/**
 * Controls state transitions within the game. For example, transitions from
 * an intro screen to main menu, menu to game level, game to pause screen, etc.
 *
 * This is a special {@link GameSystem} which is consulted at the beginning
 * of each main loop iteration.
 */
@Component
class GameStateSystem implements GameSystem {
	String name = 'gameState'
	boolean run = true

	@Override
	void update(List<GameEntity> entities) {
		log.debug "Entities: [${entities.join(',')}]"
	}

	void endGame() {
		run = false
	}
}
