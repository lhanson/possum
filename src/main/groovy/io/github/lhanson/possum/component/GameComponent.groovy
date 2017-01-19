package io.github.lhanson.possum.component

/**
 * A GameComponent is the raw data describing one aspect of a game object, and how it interacts with the world.
 * An {@link io.github.lhanson.possum.entity.GameEntity} is defined by a set of Components which define its capabilities.
 */
interface GameComponent {
	/**
	 * @return the name of this {@code GameComponent}
	 */
	String getName()
}
