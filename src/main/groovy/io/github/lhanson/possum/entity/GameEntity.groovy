package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.GameComponent

/**
 * An entity is a general-purpose game object. It possess various
 * {@link GameComponent}s which describe its capabilities and state.
 * {@code GameEntity} behavior is processed by {@link io.github.lhanson.possum.system.GameSystem}s.
 */
abstract class GameEntity {
	/**
	 * @return the name of the entity
	 */
	abstract String getName()

	/**
	 * The {@link GameComponent}s describing this entity's properties
	 */
	abstract List<GameComponent> getComponents()

	/**
	 * Returns all components belonging to this entity of the provided type
	 * @param requiredType the Class of components to be returned
	 * @return all components belonging to the entity which are instances of {@code requiredType}
	 */
	List<GameComponent> getComponentsOfType(Class requiredType) {
		components.findAll { requiredType.isInstance(it) }
	}
}
