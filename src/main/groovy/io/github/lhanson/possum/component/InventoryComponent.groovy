package io.github.lhanson.possum.component

import io.github.lhanson.possum.entity.GameEntity

/**
 * A component maintaining a list of {@link GameEntity}s in the
 * possession of the entity it belongs to.
 */
class InventoryComponent implements GameComponent {
	List<GameEntity> inventory = []

	InventoryComponent(List<GameEntity> inventory) {
		this.inventory = inventory
	}
}
