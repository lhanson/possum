package io.github.lhanson.possum.events

import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.entity.GameEntity

/**
 * Event indicating that a component was added to an entity
 */
class ComponentAddedEvent {
	/** The entity which the component is newly associated with */
	GameEntity entity
	/** The component which has been added to the entity */
	GameComponent component

	ComponentAddedEvent(GameEntity entity, GameComponent component) {
		this.entity = entity
		this.component = component
	}

	@Override
	String toString() {
		"${entity.name}, ${component.class}"
	}
}