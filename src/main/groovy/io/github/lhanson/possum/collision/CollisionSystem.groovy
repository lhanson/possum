package io.github.lhanson.possum.collision

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.MobileEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * System for handling collisions between {@link io.github.lhanson.possum.entity.GameEntity} instances.
 * Called on an as-needed basis by the {@link io.github.lhanson.possum.system.MovementSystem}
 * rather than automatically for each main loop iteration.
 */
@Component
class CollisionSystem {
	Logger log = LoggerFactory.getLogger(this.class)

	/**
	 * Process a collision between two game entities
	 * @param mobileEntity the moving entity involved in the collision
	 * @param collider the stationary game entity being collided with
	 */
	void collide(MobileEntity mobileEntity, GameEntity collider) {
		log.trace "Collision detected between $mobileEntity and $collider"
		collider.getComponentOfType(CollisionHandlingComponent)?.handleCollision(mobileEntity)
	}
}
