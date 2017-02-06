package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.PositionComponent
import io.github.lhanson.possum.component.VelocityComponent

class MobileEntity extends GameEntity {
	String name = 'MobileEntity'
	List<GameComponent> components
	PositionComponent position
	VelocityComponent velocity
	GameEntity entity
}

