package io.github.lhanson.possum.system

import io.github.lhanson.possum.collision.CollisionSystem
import io.github.lhanson.possum.collision.ImpassableComponent
import io.github.lhanson.possum.component.FocusedComponent
import io.github.lhanson.possum.component.PositionComponent
import io.github.lhanson.possum.component.VelocityComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.MobileEntity
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.rendering.RenderingSystem
import io.github.lhanson.possum.scene.Scene
import mikera.vectorz.Vector2
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MovementSystem extends GameSystem {
	@Autowired RenderingSystem renderingSystem
	@Autowired CollisionSystem collisionSystem
	Random random = new Random()
	Logger log = LoggerFactory.getLogger(this.class)
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
				log.trace "New velocity is {}", focusedEntity.velocity.vector2
			}
		}
		// Move entities
		mobileEntities.each { it.position.vector2.add(it.velocity.vector2) }

		mobileEntities.each { mobileEntity ->
			List<GameEntity> colliders = findAt(scene.entities, mobileEntity.position) - mobileEntity.entity
			colliders.each { collisionSystem.collide(mobileEntity, it) }
		}

		// Stop entities
		mobileEntities.each { it.velocity.vector2.setValues(0, 0) }

	}

	List<MobileEntity> findMobile(List<GameEntity> entities) {
		entities.findResults { entity ->
			PositionComponent p = entity.getComponentOfType(PositionComponent)
			VelocityComponent v = entity.getComponentOfType(VelocityComponent)
			if (p && v) {
				return new MobileEntity(entity: entity, position: p, velocity: v, components: entity.components, name: entity.name)
			}
		}
	}

	List<MobileEntity> findFocused(List<MobileEntity> entities) {
		entities.findAll { entity ->
			entity.components.find { it instanceof FocusedComponent }
		}
	}

	/**
	 * Finds all impassable entities from the given list which are
	 * located at the specified position.
	 *
	 * @param entities the list of entities to search
	 * @param position the position of interest
	 * @return the list of impassable entities at the given position
	 */
	List<GameEntity> findAt(List<GameEntity> entities, PositionComponent position) {
		entities.findAll { entity ->
			List<PositionComponent> positions = entity.getComponentsOfType(PositionComponent)
			return positions?.get(0)?.vector2 == position.vector2
		}
	}

	/**
	 * Finds all impassable entities from the given list which are
	 * located at the specified position.
	 *
	 * @param entities the list of entities to search
	 * @param position the position of interest
	 * @return the list of impassable entities at the given position
	 */
	List<GameEntity> findImpassableAt(List<GameEntity> entities, PositionComponent position) {
		entities.findAll { entity ->
			List<PositionComponent> positions = entity.getComponentsOfType(PositionComponent)
			def impassable = entity.getComponentsOfType(ImpassableComponent)
			return impassable && positions?.get(0)?.vector2 == position.vector2
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

	/**
	 * Calculates a random open (non-Impassable) space within the rectangular
	 * bounds of the provided entities.
	 *
	 * @param entities the entities defining the rectangular bounds of the
	 *        selection as well as disqualifying impassable entities
	 * @return a position among the entities, not occupied by an Impassable entity
	 */
	PositionComponent randomPassableSpaceWithin(List<GameEntity> entities) {
		List<PositionComponent> boundingBox = boundingBox(entities.collect { it.getComponentsOfType(PositionComponent)} )
		PositionComponent randomPosition
		while (!randomPosition) {
			int rx = random.nextInt(boundingBox[1].x + 1 - boundingBox[0].x) + boundingBox[0].x
			int ry = random.nextInt(boundingBox[1].y + 1 - boundingBox[0].y) + boundingBox[0].y
			PositionComponent tentativePosition = new PositionComponent(rx, ry)
			if (!findImpassableAt(entities, tentativePosition)) {
				randomPosition = tentativePosition
			} else {
				log.warn "Collision calculating random passable space, recomputing"
			}
		}
		return randomPosition
	}

	/**
	 * @param positions the list of positions to calculate a bounding box for
	 * @return a pair of positions representing the upper-left and lower-right
	 *         coordinates of the bounding box
	 */
	List<PositionComponent> boundingBox(List<PositionComponent> positions) {
		int minX = positions.collect { it.x }.flatten().min()
		int maxX = positions.collect { it.x }.flatten().max()
		int minY = positions.collect { it.y }.flatten().min()
		int maxY = positions.collect { it.y }.flatten().max()
		return [new PositionComponent(minX, minY), new PositionComponent(maxX, maxY)]
	}

}
