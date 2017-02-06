package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.FocusedComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.PositionComponent
import io.github.lhanson.possum.component.VelocityComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.scene.Scene
import mikera.vectorz.Vector2
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MovementSystem extends GameSystem {
	@Autowired RenderingSystem renderingSystem
	private Logger log = LoggerFactory.getLogger(this.class)
	String name = 'MovementSystem'

	@Override
	void update(Scene scene, double ticks) {
		def mobileEntities = findMobile(scene.entities)
		if (scene.activeInput) {
			findFocused(mobileEntities).each { focusedEntity ->
				log.trace "Applying {} to {}", scene.activeInput, focusedEntity.name
				Vector2 newVelocity = new Vector2()
				scene.activeInput.each { input ->
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
		mobileEntities.each { it.position.vector2.add(it.velocity.vector2) }

		// Stop entities
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

	/**
	 * Computes the center of the collection of provided {@code entities} and
	 * performs the appropriate transformation on each of them to relatively
	 * position them at the given coordinates.
	 *
	 * @param xPercent percentage of width away from origin
	 * @param yPercent percentage of height away from origin
	 * @param entities the list of entities to recenter
	 */
	void centerAround(int xPercent, yPercent, List<GameEntity> entities) {
		List<PositionComponent> positions = entities.findResults {
			it.getComponentsOfType(PositionComponent)
		}.flatten()
		def xs = positions.collect { it.x }
		def ys = positions.collect { it.y }
		int centerX = Math.ceil(xs.sum() / xs.size())
		int centerY = Math.ceil(ys.sum() / ys.size())
		int relX = (xPercent / 100.0f) * renderingSystem.viewportWidth
		int relY = (yPercent / 100.0f) * renderingSystem.viewportHeight
		int translateX = relX - centerX
		int translateY = relY - centerY
		positions.each { it.x += translateX; it.y += translateY }
	}

	class MobileEntity extends GameEntity {
		String name = 'MobileEntity'
		List<GameComponent> components
		PositionComponent position
		VelocityComponent velocity
	}
}
