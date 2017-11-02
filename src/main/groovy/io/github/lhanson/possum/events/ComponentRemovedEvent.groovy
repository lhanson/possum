package io.github.lhanson.possum.events

import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.entity.GameEntity

/**
 * Event indicating that a component was removed from an entity
 */
class ComponentRemovedEvent {
	/** The entity which the component was formerly associated with */
	GameEntity entity
	/** The component which was removed from the entity */
	GameComponent component

	ComponentRemovedEvent(GameEntity entity, GameComponent component) {
		this.entity = entity
		this.component = component
	}
}