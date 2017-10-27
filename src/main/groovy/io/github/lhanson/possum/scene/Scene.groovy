package io.github.lhanson.possum.scene

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.component.VelocityComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GaugeEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.RerenderEntity
import io.github.lhanson.possum.input.InputAdapter
import io.github.lhanson.possum.input.InputContext
import io.github.lhanson.possum.input.MappedInput
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A Scene encapsulates entities and input representing
 * a particular segment of a game.
 */
class Scene {
	Logger log = LoggerFactory.getLogger(this.class)
	InputAdapter inputAdapter
	Map<Class, List<GameEntity>> entitiesByComponentType = [:]
	// A list of entities modified in such a way as to require re-rendering
	List<GameEntity> entitiesToBeRendered = []

	/** Unique identifier for this scene */
	String id
	/** Top-level entities active in this scene, does not include entities in inventories */
	List<GameEntity> entities = []
	/** Input contexts for this scene */
	List<InputContext> inputContexts = []
	/** The input collected for this scene to process */
	Set<MappedInput> activeInput = []
	/** Whether the simulation is paused */
	volatile boolean paused = false


	Scene(String id, List<GameEntity> entities, List<InputContext> inputContexts = []) {
		log.debug "Initializing scene $id"
		this.id = id
		this.entities = entities
		this.inputContexts = inputContexts

		// Initialize lookup map of entities by component type
		entities.each { entity ->
			entity.components.each { component ->
				if (entitiesByComponentType[component.class] == null) {
					entitiesByComponentType[component.class] = []
				}
				entitiesByComponentType[component.class] << entity
			}
		}
		entitiesToBeRendered.addAll entities // All entities will need to be rendered initially
	}

	/**
	 * Returns a single unique entity matching the provided component types.
	 * If there are more than one matching entities, an error will be logged but
	 * we won't throw an exception.
	 *
	 * @param componentTypes the component types which matching entities will contain
	 * @return the matching entity, or null if none match
	 */
	GameEntity getEntityMatching(List<Class> componentTypes) {
		def results = getEntitiesMatching(componentTypes)
		if (results.size() > 1) {
			log.error "Expecting at most one unique match for $componentTypes, found ${results.size()}"
		}
		return results[0]
	}

	/**
	 * @param componentTypes the component types which matching entities will contain
	 * @return the list of entities which contain all of the specified component types.
	 */
	List<GameEntity> getEntitiesMatching(List<Class> componentTypes) {
		def matches = []
		// Find entities containing components of the given types
		Map<GameEntity, Integer> matchingComponentCountByEntity = [:]
		componentTypes.each { componentType ->
			entitiesByComponentType[componentType].each { GameEntity entity ->
				if (matchingComponentCountByEntity[entity] == null) {
					matchingComponentCountByEntity[entity] = 0
				}
				matchingComponentCountByEntity[entity]++
			}
			// Match on entities containing all of the desired component types
			matchingComponentCountByEntity.each { key, value ->
				if (value >= componentTypes.size()) {
					matches << key
				}
			}
		}
		return matches
	}

	/**
	 * @return GaugeEntities in the scene, from both the top-level entities list
	 *         as well as entities nested in {@code InventoryComponents}
	 */
	List<GaugeEntity> getGauges() {
		def results = entities.findAll { it instanceof GaugeEntity }
		def inventories = getComponents(InventoryComponent)
		def inventoryGauges = inventories?.findResults { InventoryComponent ic ->
			ic.inventory.findAll { it instanceof GaugeEntity }
		}?.flatten()
		if (inventoryGauges) {
			results.addAll(inventoryGauges)
			results.flatten()
		}
		return results
	}

	/**
	 * @param componentType the type of component to search for
	 * @return all components of the given type within the scene
	 */
	List<GameComponent> getComponents(Class componentType) {
		entitiesByComponentType[componentType]?.collect {
			it.getComponentOfType(componentType)
		}
	}

	/**
	 * @return all movable {@link GameEntity} objects
	 */
	List<GameEntity> getMobileEntities() {
		getEntitiesMatching([AreaComponent, VelocityComponent])
	}

	/**
	 * Finds all non-Panel entities located within the given area
	 * @param area the boundaries for which we want to find entities
	 * @return any entities within the provided area
	 */
	List<GameEntity> findNonPanelWithin(AreaComponent area) {
		// TODO: Optimize this
		entities.findAll { entity ->
			!(entity instanceof PanelEntity) &&
				entity.getComponentOfType(AreaComponent)?.overlaps(area)
		}
	}

	/**
	 * @param entity an entity which has been updated such that it needs to be re-rendered
	 */
	void entityNeedsRendering(GameEntity entity, AreaComponent previousArea = null) {
		entitiesToBeRendered << entity
		if (previousArea) {
			// Need to repaint what's at the entity's previous location
			def uncoveredEntities = findNonPanelWithin(previousArea)
			if (uncoveredEntities) {
				entitiesToBeRendered.addAll(uncoveredEntities)
			} else {
				// Nothing is there, use our default "background" entity
				entitiesToBeRendered << new RerenderEntity(
						name: 'backgroundRenderingEntity',
						components: [
								new AreaComponent(previousArea.x, previousArea.y, previousArea.width, previousArea.height),
								new TextComponent(' ')
						])
			}
		}
	}

	/**
	 * Collects raw input from the available {@link InputAdapter} and passes it to each
	 * {@link InputContext} within the current scene for mapping into
	 * platform-agnostic, high-level events to be fed into game logic.
	 */
	void processInput() {
		inputAdapter.collectInput()?.each { input ->
			MappedInput mapped = inputContexts.findResult { context ->
				context.mapInput(input)
			}
			if (mapped) {
				activeInput << mapped
			}
		}
	}

}
