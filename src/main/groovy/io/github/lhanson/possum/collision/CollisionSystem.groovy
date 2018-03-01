package io.github.lhanson.possum.collision

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.events.CollisionEvent
import io.github.lhanson.possum.events.EventBroker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * System for handling collisions between {@link io.github.lhanson.possum.entity.GameEntity} instances.
 * Called on an as-needed basis by the {@link io.github.lhanson.possum.system.MovementSystem}
 * rather than automatically for each main loop iteration.
 */
@Component
class CollisionSystem {
	@Autowired EventBroker eventBroker
	Logger log = LoggerFactory.getLogger(this.class)

	/**
	 * Process a collision between two game entities
	 * @param entity the moving entity involved in the collision
	 * @param collider the stationary game entity being collided with
	 */
	void collide(GameEntity entity, GameEntity collider) {
		log.trace "Collision detected between $entity and $collider"
		eventBroker.publish(new CollisionEvent(entity, collider))
		collider.getComponentOfType(CollisionHandlingComponent)?.handleCollision(entity)
	}
}
