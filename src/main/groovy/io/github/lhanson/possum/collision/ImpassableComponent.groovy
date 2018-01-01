package io.github.lhanson.possum.collision

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.VelocityComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.events.EntityMovedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Collision handling component for an impassible entity
 */
class ImpassableComponent implements CollisionHandlingComponent {
	Logger log = LoggerFactory.getLogger(this.class)

	@Override
	void handleCollision(GameEntity collidingEntity) {
		// Reverse last move
		AreaComponent ac = collidingEntity.getComponentOfType(AreaComponent)
		VelocityComponent vc = collidingEntity.getComponentOfType(VelocityComponent)
		log.trace "Collision detected between {} and {}, reversing position with vector {}",
				collidingEntity, this, vc.vector3
		AreaComponent original = new AreaComponent(ac)
		ac.position.vector3.sub(vc.vector3)
		collidingEntity.eventBroker.publish(new EntityMovedEvent(collidingEntity, original, ac))
	}

}
