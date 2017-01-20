package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.TimerComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.gameState.GameState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TimerSystem implements GameSystem {
	String name = 'timerSystem'
	@Autowired
	GameState gameState

	@Override
	void update(List<GameEntity> entities) {
		entities.each { entity ->
			entity.components
					.findAll { it instanceof TimerComponent }
					.each { TimerComponent tc ->
						log.trace "TimerComponent, ticks left: ${tc.ticksRemaining}"
						tc.ticksRemaining -= gameState.elapsedTicks
						if (tc.ticksRemaining <= 0) {
							tc.alarm()
							entity.components.remove(tc)
						}
					}
		}
	}
}
