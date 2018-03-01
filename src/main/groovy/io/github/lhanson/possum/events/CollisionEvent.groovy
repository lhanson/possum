package io.github.lhanson.possum.events

import io.github.lhanson.possum.entity.GameEntity

/**
 * Event indicating that two entities have collided
 */
class CollisionEvent {
	/** The moving entity involved in the collision */
	GameEntity entity
	/** The stationary entity being collided with */
	GameEntity collider

	CollisionEvent(GameEntity entity, GameEntity collider) {
		this.entity = entity
		this.collider = collider
	}
}
