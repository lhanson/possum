package io.github.lhanson.possum.system

import io.github.lhanson.possum.collision.CollisionSystem
import io.github.lhanson.possum.collision.ImpassableComponent
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.PlayerInputAwareComponent
import io.github.lhanson.possum.component.PositionComponent
import io.github.lhanson.possum.component.VelocityComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.events.EntityMovedEvent
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.rendering.RenderingSystem
import io.github.lhanson.possum.scene.Scene
import mikera.vectorz.Vector3
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MovementSystem extends GameSystem {
	@Autowired RenderingSystem renderingSystem
	@Autowired CollisionSystem collisionSystem
	@Autowired Random random
	@Autowired EventBroker eventBroker
	Logger log = LoggerFactory.getLogger(this.class)
	String name = 'MovementSystem'
	VelocityComponent still = new VelocityComponent(0, 0, 0)
	Set<GameEntity> movingEntities

	@Override
	void doInitScene(Scene scene) {
		movingEntities = scene
				.getEntitiesMatching([VelocityComponent])
				.findAll { it.getComponentOfType(VelocityComponent) != still }
	}

	@Override
	void doUpdate(Scene scene, double ticks) {
		if (scene.activeInput) {
			log.trace("Updating movements for {} active inputs", scene.activeInput.size())
			scene.getEntitiesMatching([PlayerInputAwareComponent]).each { entity ->
				log.trace "Applying {} to {}", scene.activeInput, entity.name
				Vector3 newVelocity = new Vector3()
				scene.activeInput.each { input ->
					switch (input) {
						case (MappedInput.UP_LEFT):
							newVelocity.add(-1, -1, 0)
							break
						case (MappedInput.UP):
							newVelocity.add(0, -1, 0)
							break
						case (MappedInput.UP_RIGHT):
							newVelocity.add(1, -1, 0)
							break
						case (MappedInput.DOWN_LEFT):
							newVelocity.add(-1, 1, 0)
							break
						case (MappedInput.DOWN):
							newVelocity.add(0, 1, 0)
							break
						case (MappedInput.DOWN_RIGHT):
							newVelocity.add(1, 1, 0)
							break
						case (MappedInput.LEFT):
							newVelocity.add(-1, 0, 0)
							break
						case (MappedInput.RIGHT):
							newVelocity.add(1, 0, 0)
							break
					}
				}
				VelocityComponent velocity = entity.getComponentOfType(VelocityComponent)
				velocity.vector3.add(newVelocity)
				if (velocity != still) {
					movingEntities.add(entity)
				}
			}
		}

		// Move entities
		movingEntities.each {
			AreaComponent ac = it.getComponentOfType(AreaComponent)
			VelocityComponent vc = it.getComponentOfType(VelocityComponent)
			if (vc != still) {
				def oldPos = new AreaComponent(ac)
				ac.position.vector3.add(vc.vector3)
				log.trace "Entity {} moved from {} to {}", it.name, oldPos.position, ac.position
				scene.entityNeedsRendering(it, oldPos)
				eventBroker.publish(new EntityMovedEvent(it, oldPos, ac))
			}
		}

		// Resolve collisions
		movingEntities.each { entity ->
			AreaComponent location = entity.getComponentOfType(AreaComponent)
			List<GameEntity> colliders = scene.findNonPanelWithin(location) - entity
			colliders.each {
				if (!(it instanceof PanelEntity)) {
					collisionSystem.collide(entity, it)
					log.trace "Collided {} and {}", entity, it
					scene.entityNeedsRendering(entity)
					scene.entityNeedsRendering(it)
				}
			}
		}

		// Stop entities
		movingEntities.each { it.getComponentOfType(VelocityComponent).vector3.setValues(0, 0, 0) }
		movingEntities.clear()
	}

	/**
	 * Finds all impassable entities from the given list which are
	 * located at the specified position.
	 *
	 * @param entities the list of entities to search
	 * @param position the position of interest
	 * @return the list of impassable entities at the given position
	 */
	List<GameEntity> findImpassableAt(List<GameEntity> entities, AreaComponent position) {
		entities.findAll { entity ->
			List<PositionComponent> positions = entity.getComponentsOfType(AreaComponent)
			def impassable = entity.getComponentsOfType(ImpassableComponent)
			return impassable && positions?.get(0) == position
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
	 * @return an area among the entities, not occupied by an Impassable entity
	 */
	AreaComponent randomPassableSpaceWithin(List<GameEntity> entities) {
		AreaComponent boundingBox = boundingBox(entities.collect { it.getComponentsOfType(AreaComponent)} )
		AreaComponent randomPosition
		while (!randomPosition) {
			int rx = random.nextInt(boundingBox.width + 1 - boundingBox.x) + boundingBox.x
			int ry = random.nextInt(boundingBox.height + 1 - boundingBox.y) + boundingBox.y
			AreaComponent tentativePosition = new AreaComponent(rx, ry, 1, 1)
			if (!findImpassableAt(entities, tentativePosition)) {
				randomPosition = tentativePosition
			} else {
				log.warn "Collision calculating random passable space, recomputing"
			}
		}
		return randomPosition
	}

	/**
	 * @param areas the list of areas to calculate a bounding box for
	 * @return an area representing the bounding box encompassing the areas
	 */
	AreaComponent boundingBox(List<AreaComponent> areas) {
		int minX = areas.collect { it.x }.flatten().min()
		int maxX = areas.collect { it.x }.flatten().max()
		int minY = areas.collect { it.y }.flatten().min()
		int maxY = areas.collect { it.y }.flatten().max()
		return new AreaComponent(minX, minY, maxX - minX, maxY - minY)
	}

}
