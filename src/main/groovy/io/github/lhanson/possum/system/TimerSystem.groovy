package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.TimerComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.ComponentRemovedEvent
import io.github.lhanson.possum.events.Subscription
import io.github.lhanson.possum.scene.Scene
import org.springframework.stereotype.Component

@Component
class TimerSystem extends GameSystem {
	String name = 'timerSystem'
	List<GameEntity> timers

	@Override
	void doInitScene(Scene scene) {
		scene.eventBroker.subscribe(this)
		timers = scene.entities.findAll {
			it.components.findAll { it instanceof TimerComponent }
		}
	}

	@Override
	void doUpdate(Scene scene, double ticks) {
		timers.each { entity ->
			entity.getComponentsOfType(TimerComponent)
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

	@Subscription
	void componentAdded(ComponentAddedEvent event) {
		if (event.component instanceof TimerComponent) {
			log.debug("Added {} to timer lookup list", event.entity)
			timers.add(event.entity)
		}
	}

	@Subscription
	void componentRemoved(ComponentRemovedEvent event) {
		timers.remove(event.entity)
		log.debug("Removed {} from timer lookup list", event.entity)
	}

}
