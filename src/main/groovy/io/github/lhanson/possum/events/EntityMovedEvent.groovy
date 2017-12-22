package io.github.lhanson.possum.events

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GameEntity

/**
 * Event indicating that an entity has changed position
 */
class EntityMovedEvent {
	/** The entity which the component is newly associated with */
	GameEntity entity
	/** The previous location of the entity */
	AreaComponent oldPosition
	/** The new location of the entity */
	AreaComponent newPosition

	EntityMovedEvent(GameEntity entity, AreaComponent oldPosition, AreaComponent newPosition) {
		this.entity = entity
		this.oldPosition = oldPosition
		this.newPosition = newPosition
	}
}
