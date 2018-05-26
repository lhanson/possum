package io.github.lhanson.possum.component

import io.github.lhanson.possum.entity.GameEntity

/**
 * A component maintaining a list of {@link GameEntity}s in the
 * possession of the entity it belongs to.
 */
class InventoryComponent implements GameComponent {
	@Delegate
	private List<GameEntity> _inventory

	InventoryComponent(List<GameEntity> inventory) {
		if (inventory) {
			_inventory = inventory
			_inventory.each { GameEntity entity ->
				AreaComponent ac = entity.getComponentOfType(AreaComponent)
				ac.frameOfReference = AreaComponent.FrameOfReference.PARENT
			}
		} else {
			_inventory = []
		}
	}

}
