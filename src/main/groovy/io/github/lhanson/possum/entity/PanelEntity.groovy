package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.InventoryComponent

/**
 * Entity type which is treated specially by renderers.
 */
class PanelEntity extends GameEntity {
	/**
	 * The padding allowance for borders. 0 means content prints right
	 * up to the edge of the panel; you'll want more if you're rendering borders.
	 */
	int padding = 0

	@Override
	GameComponent getComponentOfType(Class requiredType) {
		if (requiredType == AreaComponent && super.getComponentOfType(requiredType)) {
			// Panels can have variable text, so we should recalculate its current area
			AreaComponent defaultArea = super.getComponentOfType(requiredType)
			InventoryComponent panelInventory = super.getComponentOfType(InventoryComponent)
			int textHeight = 0
			panelInventory.inventory.each { GameEntity entity ->
				if (entity instanceof TextEntity) {
					textHeight += entity.calculateArea().height
				}
			}
			return new AreaComponent(defaultArea.x, defaultArea.y, defaultArea.width, textHeight + 2)
		} else {
			return super.getComponentOfType(requiredType)
		}
	}

}
