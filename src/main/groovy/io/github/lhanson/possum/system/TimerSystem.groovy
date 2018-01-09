package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.TimerComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.ComponentRemovedEvent
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.events.Subscription
import io.github.lhanson.possum.scene.Scene
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TimerSystem extends GameSystem {
	@Autowired EventBroker eventBroker
	String name = 'timerSystem'
	List<GameEntity> timers
	boolean removingComponent = false

	@Override
	void doInitScene(Scene scene) {
		eventBroker.subscribe(this)
		timers = scene.entities.findAll {
			it.components.findAll { it instanceof TimerComponent }
		}
	}

	@Override
	void doUninitScene(Scene scene) {
		timers = null
		eventBroker.unsubscribe(this)
	}

	@Override
	void doUpdate(Scene scene, double ticks) {
		timers.each { entity ->
			def expiredTimers = []
			entity.getComponentsOfType(TimerComponent)
					.each { TimerComponent tc ->
				log.trace "TimerComponent, ticks left: ${tc.ticksRemaining}"
				tc.ticksRemaining -= ticks
				if (tc.ticksRemaining <= 0) {
					tc.alarm()
					expiredTimers << tc
				}
			}
			removingComponent = true
			expiredTimers.each { entity.removeComponent(it) }
			removingComponent = false
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
		if (!removingComponent && event.component instanceof TimerComponent) {
			timers.remove(event.entity)
		}
		log.debug("Removed {} from timer lookup list", event.entity)
	}

}
