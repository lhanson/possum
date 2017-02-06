package io.github.lhanson.possum.collision

import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.entity.MobileEntity

/**
 * A component which resolves a particular collision by enacting a Scene transition.
 */
interface CollisionHandlingComponent extends GameComponent {
	void handleCollision(MobileEntity collidingEntity)
}
