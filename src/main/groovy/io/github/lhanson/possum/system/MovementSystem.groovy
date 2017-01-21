package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.FocusedComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.PositionComponent
import io.github.lhanson.possum.component.VelocityComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.gameState.GameState
import io.github.lhanson.possum.input.MappedInput
import mikera.vectorz.Vector2
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MovementSystem implements GameSystem {
	private Logger log = LoggerFactory.getLogger(this.class)
	String name = 'MovementSystem'
	@Autowired GameState gameState

	@Override
	void update(List<GameEntity> entities) {
		def mobileEntities = findMobile(entities)
		if (gameState.activeInput) {
			findFocused(mobileEntities).each { focusedEntity ->
				log.trace "Applying {} to {}", gameState.activeInput, focusedEntity.name
				Vector2 newVelocity = new Vector2()
				gameState.activeInput.each { input ->
					switch (input) {
						case (MappedInput.UP):
							newVelocity.add(0, -1)
							break
						case (MappedInput.DOWN):
							newVelocity.add(0, 1)
							break
						case (MappedInput.LEFT):
							newVelocity.add(-1, 0)
							break
						case (MappedInput.RIGHT):
							newVelocity.add(1, 0)
							break
					}
				}
				focusedEntity.velocity.vector2.add(newVelocity)
				focusedEntity.velocity.vector2.normalise()
				log.trace "New velocity is {}", focusedEntity.velocity.vector2
			}
		}
		// Move entities
		// TODO: Track 'dirty' areas where movement has happened.
		// TODO: Given areas of movement, the rendering system needs to
		// TODO: know what to re-draw there.
		// TODO: Perhaps a lookup of entities by positions?
		mobileEntities.each { it.position.vector2.add(it.velocity.vector2) }

		// Stop entities
		// TODO: This works to implement basic movement for now, but
		// TODO: will not hold up for sustained movement, missiles, etc.
		// TODO: Best to develop rudimentary friction to kill off velocity
		// TODO: after one move, at least in the case of a turn-based
		// TODO: dungeon crawler.
		mobileEntities.each { it.velocity.vector2.setValues(0, 0) }
	}

	List<MobileEntity> findMobile(List<GameEntity> entities) {
		entities.findResults { entity ->
			PositionComponent p = entity.components.find { it instanceof PositionComponent }
			VelocityComponent v = entity.components.find { it instanceof VelocityComponent }
			if (p && v) {
				return new MobileEntity(position: p, velocity: v, components: entity.components, name: entity.name)
			}
		}
	}

	List<MobileEntity> findFocused(List<MobileEntity> entities) {
		entities.findAll { entity ->
			entity.components.find { it instanceof FocusedComponent }
		}
	}

	class MobileEntity implements GameEntity {
		String name = 'MobileEntity'
		List<GameComponent> components
		PositionComponent position
		VelocityComponent velocity
	}
}
