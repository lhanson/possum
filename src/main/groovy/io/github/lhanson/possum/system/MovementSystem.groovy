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
	final VelocityComponent still = new VelocityComponent(0, 0, 0)
	Map<String, Set<GameEntity>> movingEntities = [:]

	@Override
	void doInitScene(Scene scene) {
		movingEntities[scene.id] = scene
				.getEntitiesMatching([VelocityComponent])
				.findAll { it.getComponentOfType(VelocityComponent) != still }
		println "Initialized ${movingEntities[scene.id]}"
	}

	@Override
	void doUninitScene(Scene scene) {
		movingEntities[scene.id] = null
	}

	@Override
	void doUpdate(Scene scene, double ticks) {
		if (scene.activeInput) {
			log.trace("Updating movements for {} active inputs", scene.activeInput.size())
			scene.getEntitiesMatching([PlayerInputAwareComponent, VelocityComponent]).each { entity ->
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
					if (movingEntities[scene.id] == null) {
						movingEntities[scene.id] = [] as Set
					}
					movingEntities[scene.id].add(entity)
				}
			}
		}

		// Move entities
		movingEntities[scene.id].each {
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
		movingEntities[scene.id].each { entity ->
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
		movingEntities[scene.id].each {
			it.getComponentOfType(VelocityComponent).vector3.setValues(0, 0, 0)
		}
		movingEntities[scene.id]?.clear()
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
