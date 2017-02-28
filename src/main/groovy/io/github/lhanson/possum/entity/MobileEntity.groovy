package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.PositionComponent
import io.github.lhanson.possum.component.VelocityComponent

class MobileEntity extends GameEntity {
	String name = 'MobileEntity'
	List<GameComponent> components
	PositionComponent position
	VelocityComponent velocity

	/**
	 * Creates a MobileEntity from the provided base GameEntity
	 */
	MobileEntity(GameEntity baseEntity) {
		name = baseEntity.name
		position = baseEntity.getComponentOfType(PositionComponent)
		velocity = baseEntity.getComponentOfType(VelocityComponent)
	}
}

