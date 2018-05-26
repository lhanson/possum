package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GaugeEntity
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.ComponentRemovedEvent
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.events.Subscription
import io.github.lhanson.possum.scene.Scene
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class GaugeSystem extends GameSystem {
	@Autowired EventBroker eventBroker
	String name = 'GaugeSystem'
	Map<String, List<GaugeEntity>> gauges = [:]

	@Override
	void doInitScene(Scene scene) {
		eventBroker.subscribe(this)

		// Locate GaugeEntities in the scene
		gauges[scene.id] = scene.entities.findAll { it instanceof GaugeEntity }
	}

	@Override
	void doUninitScene(Scene scene) {
		gauges.remove(scene.id)
		eventBroker.unsubscribe(this)
	}

	@Override
	void doUpdate(Scene scene, double elapsed) {
		gauges[scene.id].each { GaugeEntity gauge ->
			def beforeText = gauge.text
			def previousArea = new AreaComponent(gauge.getComponentOfType(AreaComponent))
			gauge.update(elapsed)
			if (gauge.text != beforeText) {
				log.trace "Gauge {} value changed in scene {}", gauge.name, scene.id
				scene.entityNeedsRendering(gauge, previousArea)
			}
		}
	}

	@Subscription
	void componentAdded(ComponentAddedEvent event) {
		if (event.entity instanceof GaugeEntity) {
			log.debug("Added {} to gauge lookup list for scene {}", event.entity, event.entity.scene)
			if (gauges[event.entity.scene.id] == null) {
				gauges[event.entity.scene.id] = []
			}
			gauges[event.entity.scene.id].add(event.entity)
		}
	}

	@Subscription
	void componentRemoved(ComponentRemovedEvent event) {
		if (event.entity instanceof GaugeEntity) {
			gauges[event.entity.scene.id].remove(event.entity)
		}
		log.debug("Removed {} from gauge lookup list for scene {}", event.entity, event.entity.scene)
	}

}
