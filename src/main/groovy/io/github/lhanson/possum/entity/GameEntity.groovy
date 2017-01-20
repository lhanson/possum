package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.GameComponent

/**
 * An entity is a general-purpose game object. It possess various
 * {@link GameComponent}s which describe its capabilities and state.
 * {@code GameEntity} behavior is processed by {@link io.github.lhanson.possum.system.GameSystem}s.
 */
interface GameEntity {
	/**
	 * @return the name of the entity
	 */
	String getName()

	/**
	 * The {@link GameComponent}s describing this entity's properties
	 */
	List<GameComponent> getComponents()
}
