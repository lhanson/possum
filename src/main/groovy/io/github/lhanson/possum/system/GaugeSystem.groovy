package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.InventoryComponent
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
	List<GaugeEntity> gauges

	@Override
	void doInitScene(Scene scene) {
		eventBroker.subscribe(this)

		// Locate GaugeEntities in the scene, from both the top-level entities list
		//  as well as entities nested in InventoryComponents
		gauges = scene.entities.findAll { it instanceof GaugeEntity }
		def inventories = scene.getComponents(InventoryComponent)
		def inventoryGauges = inventories?.findResults { InventoryComponent ic ->
			ic.inventory.findAll { it instanceof GaugeEntity }
		}?.flatten()
		if (inventoryGauges) {
			gauges.addAll(inventoryGauges)
			gauges.flatten()
		}
	}

	@Override
	void doUpdate(Scene scene, double elapsed) {
		gauges.each { GaugeEntity gauge ->
			def before = gauge.text
			gauge.update(elapsed)
			if (gauge.text != before) {
				log.trace "Gauge {} value changed", gauge.name
				scene.entityNeedsRendering(gauge)
			}
		}
	}

	@Subscription
	void componentAdded(ComponentAddedEvent event) {
		if (event.entity instanceof GaugeEntity) {
			log.debug("Added {} to gauge lookup list", event.entity)
			gauges.add(event.entity)
		}
	}

	@Subscription
	void componentRemoved(ComponentRemovedEvent event) {
		if (event.entity instanceof GaugeEntity) {
			gauges.remove(event.entity)
		}
		log.debug("Removed {} from gauge lookup list", event.entity)
	}

}
