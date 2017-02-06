package io.github.lhanson.possum.collision

import io.github.lhanson.possum.entity.MobileEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Collision handling component for an impassible entity
 */
class ImpassableComponent implements CollisionHandlingComponent {
	Logger log = LoggerFactory.getLogger(this.class)

	@Override
	void handleCollision(MobileEntity collidingEntity) {
		// Reverse last move
		log.trace "Collision detected between {} and {}, reversing position with vector {}",
				collidingEntity, this, collidingEntity.velocity.vector2
		collidingEntity.position.vector2.sub(collidingEntity.velocity.vector2)
	}
}
