package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.TimerComponent
import io.github.lhanson.possum.scene.Scene
import org.springframework.stereotype.Component

@Component
class TimerSystem extends GameSystem {
	String name = 'timerSystem'

	@Override
	void doUpdate(Scene scene, double ticks) {
		scene.entities.each { entity ->
			entity.components
					.findAll { it instanceof TimerComponent }
					.each { TimerComponent tc ->
						log.trace "TimerComponent, ticks left: ${tc.ticksRemaining}"
						tc.ticksRemaining -= ticks
						if (tc.ticksRemaining <= 0) {
							tc.alarm()
							entity.components.remove(tc)
						}
					}
		}
	}
}
