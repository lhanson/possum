package io.github.lhanson.possum.events

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GameEntity

/**
 * Event indicating that an entity is queued for rendering in the current frame.
 */
class EntityPreRenderEvent {
	/** The entity which is queued for rendering */
	GameEntity entity
	/** If the entity is moving, this will be the area it is moving from */
	AreaComponent previousArea

	EntityPreRenderEvent(GameEntity entity, AreaComponent previousArea) {
		this.entity = entity
		this.previousArea = previousArea
	}
}
