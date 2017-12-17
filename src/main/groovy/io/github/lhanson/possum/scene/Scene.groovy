package io.github.lhanson.possum.scene

import io.github.lhanson.possum.collision.Quadtree
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.RerenderEntity
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.ComponentRemovedEvent
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.events.Subscription
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
	// Top-level entities active in this scene, does not include entities in inventories
	private List<GameEntity> entities = []
	private Map<Class, List<GameEntity>> entitiesByComponentType = [:]
	// A set of entities modified in such a way as to require re-rendering
	private Set<GameEntity> entitiesToBeRendered = []
	Quadtree quadtree

	/** Unique identifier for this scene */
	String id
	/** Input contexts for this scene */
	List<InputContext> inputContexts = []
	/** The input collected for this scene to process */
	Set<MappedInput> activeInput = []
	/** Event broker for this scene */
	EventBroker eventBroker = new EventBroker()
	/** Whether the simulation is in debug mode */
	volatile boolean debug = false
	/** If we're in debug mode, how long to pause while showing rendering hints */
	volatile int debugPauseMillis = 1000
	/** Whether the simulation is paused */
	volatile boolean paused = false


	Scene(String id, List<GameEntity> entities, List<InputContext> inputContexts = []) {
		log.debug "Initializing scene $id"
		this.id = id
		setEntities(entities)
		this.inputContexts = inputContexts

		eventBroker.subscribe(this)

		// All entities will need to be rendered initially
		entitiesToBeRendered.addAll entities
	}

	void setEntities(List<GameEntity> entities) {
		this.entities = entities
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE,
			maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE
		entitiesByComponentType.clear()
		entities.each { entity ->
			entity.eventBroker = eventBroker
			entity.components.each { component ->
				if (entitiesByComponentType[component.class] == null) {
					entitiesByComponentType[component.class] = []
				}
				entitiesByComponentType[component.class] << entity

				if (component instanceof AreaComponent) {
					if (component.x < minX) minX = component.x
					if (component.y < minY) minY = component.y
					if (component.x + component.width > maxX) maxX = component.x + component.width
					if (component.y + component.height > maxY) maxY = component.y + component.height
				}
			}
		}
		quadtree = new Quadtree(new AreaComponent(minX, minY, maxX, maxY))

		if (entitiesByComponentType[AreaComponent]) {
			def areaEntities = entitiesByComponentType[AreaComponent].size()
			log.debug "Initializing quadtree with $areaEntities area entities with bounds [$minX, $minY] to [$maxX, $maxY], total grid space of ${(maxX - minX) * (maxY - minY)} locations"
			long start = System.currentTimeMillis()
			entitiesByComponentType[AreaComponent].each { quadtree.insert it }
			log.debug("Initialized quadtree in {} ms", System.currentTimeMillis() - start)
		}
	}

	@Subscription
	void componentAdded(ComponentAddedEvent event) {
		if (entitiesByComponentType[event.component.class] == null) {
			entitiesByComponentType[event.component.class] = []
		}
		entitiesByComponentType[event.component.class] << event.entity
		log.debug("Added {} to component lookup list for {}", event.entity, event.component)
	}

	@Subscription
	void componentRemoved(ComponentRemovedEvent event) {
		entitiesByComponentType[event.component.class].remove(event.entity)
		log.debug("Removed {} from component lookup list for {}", event.entity, event.component)
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
	 * Returns all entities in this scene with components of the given type.
	 * NOTE: This can be a big performance bottleneck if used indiscriminately
	 *       on large collections of entities.
	 *
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
	 * @param componentType the type of component to search for
	 * @return all components of the given type within the scene
	 */
	List<GameComponent> getComponents(Class componentType) {
		entitiesByComponentType[componentType]?.collect {
			it.getComponentOfType(componentType)
		}
	}

	/**
	 * Finds all non-Panel entities located within the given area
	 * @param area the boundaries for which we want to find entities
	 * @return any entities within the provided area
	 */
	List<GameEntity> findNonPanelWithin(AreaComponent area) {
		long start = System.currentTimeMillis()
		def result = quadtree.retrieve(area).findAll { !(it instanceof PanelEntity) }
		if (log.isTraceEnabled()) {
			log.trace "Finding {} non-panels within {} took {} ms", result.size(), area, System.currentTimeMillis() - start
		}
		return result
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
