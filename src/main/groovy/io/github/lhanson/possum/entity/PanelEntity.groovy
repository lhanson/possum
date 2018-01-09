package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.RelativeWidthComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Representing a visible UI container, PanelEntity is treated specially by renderers.
 * AreaComponent and InventoryComponent are guaranteed to be present, so  defaults are
 * generated if required.
 */
class PanelEntity extends GameEntity {
	Logger log = LoggerFactory.getLogger(this.class)
	// Local references to components we need easy access to
	private AreaComponent areaComponent
	private InventoryComponent inventoryComponent

	/**
	 * The padding allowance for borders. 0 means content prints right
	 * up to the edge of the panel; you'll want more if you're rendering borders.
	 */
	int padding = 1

	@Override
	void init() {
		if (initialized) {
			log.debug "Already initialized $name, skipping"
			return
		}
		super.init()

		// areaComponent relies on inventoryComponent for its calculations,
		// so initialize this first
		if (getComponentOfType(InventoryComponent)) {
			inventoryComponent = getComponentOfType(InventoryComponent)
			inventoryComponent.inventory.eachWithIndex { entity, i ->
				entity.init()
				entity.getComponentOfType(AreaComponent)?.x = padding
				entity.getComponentOfType(AreaComponent)?.y = padding + i
			}
		} else {
			log.debug "No InventoryComponent found for panel $name on initialization, adding one"
			inventoryComponent = new InventoryComponent()
			components.add(inventoryComponent)
		}

		if (getComponentOfType(RelativeWidthComponent) && !getComponentOfType(RelativePositionComponent)) {
			log.debug "Found RelativeWidthComponent but no RelativePositionComponent for panel $name on initialization, adding one"
			components.add(new RelativePositionComponent(50, 50))
		}

		if (getComponentOfType(AreaComponent)) {
			areaComponent = getComponentOfType(AreaComponent)
		} else {
			log.debug "No AreaComponent found for panel $name on initialization, adding one"
			areaComponent = new AreaComponent()
			components.add(areaComponent)
		}
		computeArea()
	}

	/**
	 * Panel contents can change, so recalculate area based on current inventory
	 */
	void computeArea() {
		int width = 0
		int height = 0
		inventoryComponent.inventory.each { GameEntity e ->
			AreaComponent itemArea = e.getComponentOfType(AreaComponent)
			if (itemArea.width > width) {
				width = itemArea.width
			}
			height++
		}
		areaComponent.width = width + (padding * 2)
		areaComponent.height = height + (padding * 2)
	}

}
