package io.github.lhanson.possum.component

import io.github.lhanson.possum.entity.GameEntity

/**
 * A component maintaining a list of {@link GameEntity}s in the
 * possession of the entity it belongs to.
 */
class InventoryComponent implements GameComponent {
	List<GameEntity> inventory

	InventoryComponent(List<GameEntity> inventory) {
		if (inventory) {
			this.inventory = inventory
			this.inventory.each { GameEntity entity ->
				AreaComponent ac = entity.getComponentOfType(AreaComponent)
				ac.frameOfReference = AreaComponent.FrameOfReference.PARENT
			}
		} else {
			this.inventory = []
		}
	}

}
