package io.github.lhanson.possum.entity

/**
 * An entity is a general-purpose game object. It possess various {@link io.github.lhanson.possum.component.GameComponent}s which
 * describe its capabilities and state. GameEntity behavior is processed by {@link io.github.lhanson.possum.system.GameSystem}s.
 */
interface GameEntity {
	/**
	 * @return the name of the entity
	 */
	String getName()

	/**
	 * Unique identifier for this entity
	 */
	int getId()
}
