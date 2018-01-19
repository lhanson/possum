package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.RelativeWidthComponent
import io.github.lhanson.possum.events.SceneInitializedEvent
import io.github.lhanson.possum.events.Subscription
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Representing a visible UI container, PanelEntity is treated specially by renderers.
 * AreaComponent and InventoryComponent are guaranteed to be present, so defaults are
 * generated if required.
 */
class PanelEntity extends GameEntity {
	Logger log = LoggerFactory.getLogger(this.class)
	// Local references to components we need easy access to
	private AreaComponent areaComponent
	InventoryComponent inventoryComponent

	/**
	 * The padding allowance for borders. 0 means content prints right
	 * up to the edge of the panel; you'll want more if you're rendering borders.
	 */
	int padding = 1

	PanelEntity() {
		this(null)
	}

	PanelEntity(GameEntity gameEntity) {
		this([gameEntity])
	}

	PanelEntity(List<GameEntity> panelEntities) {
		super()
		this.inventoryComponent = new InventoryComponent(panelEntities)
		components.add(inventoryComponent)
	}

	@Override
	void setComponents(List<GameComponent> components) {
		super.setComponents(components)
		ensureAreaComponent()
		ensureInventoryComponent()
		ensureRelativePosition()
		computeInventoryPositions()
		computeArea()
	}

	@Subscription
	void sceneInitialized(SceneInitializedEvent event) {
		computeInventoryPositions()
	}

	@Override
	GameComponent getComponentOfType(Class requiredType) {
		def result = super.getComponentOfType(requiredType)
		if (!result && requiredType == InventoryComponent) {
			result = ensureInventoryComponent()
		} else if (!result && requiredType == AreaComponent) {
			result = ensureAreaComponent()
		}
		return result
	}

	/**
	 * Panel contents can change, so recalculate area based on current inventory
	 */
	private void computeArea() {
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

	// We guarantee an InventoryComponent is present, create one if needed
	private InventoryComponent ensureInventoryComponent() {
		if (!inventoryComponent) {
			log.debug "No InventoryComponent found for panel entity $name on initialization, adding one"
			inventoryComponent = new InventoryComponent()
			components << inventoryComponent
		}
		if (!areaComponent) {
			ensureAreaComponent()
			computeArea()
		}
		return inventoryComponent
	}

	// We guarantee an AreaComponent is present, create one if needed
	private AreaComponent ensureAreaComponent() {
		AreaComponent ac = super.getComponentOfType(AreaComponent)
		if (!ac) {
			log.debug "No AreaComponent found for panel entity $name on initialization, adding one"
			ac = new AreaComponent()
			components << ac
		}
		areaComponent = ac
		if (!inventoryComponent) {
			ensureInventoryComponent()
		}
		computeArea()
		return areaComponent
	}

	private void ensureRelativePosition() {
		if (getComponentOfType(RelativeWidthComponent) &&
				!getComponentOfType(RelativePositionComponent)) {
			log.debug "Found RelativeWidthComponent but no RelativePositionComponent for panel $name on initialization, adding one"
			components.add(new RelativePositionComponent(50, 50))
		}
	}

	private void computeInventoryPositions() {
		inventoryComponent.inventory.eachWithIndex { entity, i ->
			entity.getComponentOfType(AreaComponent)?.x = padding
			entity.getComponentOfType(AreaComponent)?.y = padding + i
		}
	}

}
