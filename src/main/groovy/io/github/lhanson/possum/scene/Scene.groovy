package io.github.lhanson.possum.scene

import groovy.transform.ToString
import io.github.lhanson.possum.collision.Quadtree
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.RerenderEntity
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.ComponentRemovedEvent
import io.github.lhanson.possum.events.EntityMovedEvent
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.events.SceneInitializedEvent
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
@ToString(includes = 'id')
class Scene {
	InputAdapter inputAdapter
	/** Unique identifier for this scene */
	String id
	/** The input collected for this scene to process */
	Set<MappedInput> activeInput = []
	/** A scene to run while we do initialization */
	Scene loadingScene
	/** Event broker for this scene */
	EventBroker eventBroker
	/** Whether the scene has been initialized yet */
	boolean initialized = false
	/** Whether the simulation is in debug mode */
	volatile boolean debug = false
	/** If we're in debug mode, how long to pause while showing rendering hints */
	volatile int debugPauseMillis = 1000
	/** Whether the simulation is paused */
	volatile boolean paused = false

	List<GameEntity> panels = []
	private Logger log = LoggerFactory.getLogger(this.class)
	// Initialize (or reinitialize) the scene
	private SceneInitializer sceneInitializer
	// Top-level entities active in this scene, does not include entities in inventories
	private List<GameEntity> entities = []
	// Input contexts for this scene
	private List<InputContext> inputContexts = []
	private Map<Class, List<GameEntity>> entitiesByComponentType = [:]
	// A set of entities modified in such a way as to require re-rendering
	private SortedSet<GameEntity> entitiesToBeRendered = new TreeSet({ GameEntity a, GameEntity b ->
		AreaComponent acA = a.getComponentOfType(AreaComponent)
		AreaComponent acB = b.getComponentOfType(AreaComponent)
		// Order by z-axis if they differ, otherwise use natural object comparator
		return acA?.position?.z <=> acB?.position?.z ?: a <=> b
	})
	private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE,
	            maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE
	Quadtree quadtree = new Quadtree()


	/**
	 * Constructor for simple scenes with no input.
	 *
	 * @param id the ID of the scene
	 */
	Scene(String id, SceneInitializer sceneInitializer = null) {
		this(id, sceneInitializer, null)
	}

	/**
	 * Constructor which takes an initialization Runnable for scenes with expensive
	 * initialization requirements (many entities, for example) or which may need to
	 * be reinitialized multiple times.
	 *
	 * @param id the ID of the scene
	 * @param sceneInitializer the initialization code to execute when (re-)initializing the scene
	 * @param inputContexts input contexts for handling this scene's input
	 * @param loadingScene if supplied, a scene which will run while this one's initialization occurs
	 */
	Scene(String id, SceneInitializer sceneInitializer, List<InputContext> inputContexts, Scene loadingScene = null) {
		log.debug "Creating scene '$id' with {} input contexts", inputContexts?.size() ?: 0
		long startTime = System.currentTimeMillis()

		this.id = id
		this.inputContexts = inputContexts
		this.sceneInitializer = sceneInitializer
		this.loadingScene = loadingScene

		log.debug "Created scene '{}' with in {} ms", id, System.currentTimeMillis() - startTime
	}

	/**
	 * Initializes the scene. Executes common init code as well
	 * as the scene-specific {@link SceneInitializer}.
	 */
	void init() {
		log.debug "Initializing scene '$id'"
		long startTime = System.currentTimeMillis()

		if (!eventBroker) throw new IllegalStateException("No event broker for scene $id")
		eventBroker.subscribe(this)

		setEntities(sceneInitializer?.initScene())

		// Initialize quadtree
		quadtree = new Quadtree(new AreaComponent(minX, minY, maxX, maxY))
		if (entitiesByComponentType[AreaComponent]) {
			def areaEntities = entitiesByComponentType[AreaComponent].size()
			log.debug "Initializing quadtree with $areaEntities area entities with bounds [$minX, $minY] to [$maxX, $maxY], total grid space of ${(maxX - minX) * (maxY - minY)} locations"
			long start = System.currentTimeMillis()
			entitiesByComponentType[AreaComponent].each { quadtree.insert it }
			log.debug("Initialized quadtree in {} ms", System.currentTimeMillis() - start)
		}

		// The whole viewport will need to be rendered initially
		entitiesToBeRendered << new RerenderEntity()

		initialized = true
		log.debug "Initialized scene '{}' in {} ms", id, System.currentTimeMillis() - startTime
		eventBroker.publish(new SceneInitializedEvent(id))
	}

	/**
	 * Uninitializes a scene. When a scene is completed, this will free up
	 * any resources it may be holding.
	 */
	void uninit() {
		if (loadingScene) {
			loadingScene.uninit()
		}
		log.debug "Uninitializing scene '$id'"
		entities.clear()
		panels.clear()
		entitiesByComponentType.clear()
		entitiesToBeRendered.clear()
		eventBroker.unsubscribe(this)
		initialized = false
	}

	void setEntities(List<GameEntity> entities) {
		this.entities.clear()
		panels.clear()
		entitiesByComponentType.clear()
		entities.each { addEntity(it) }
	}

	/**
	 * Adds the entity to the scene.
	 *
	 * @param entity the entity to add to this scene
	 */
	void addEntity(GameEntity entity) {
		entities << entity
		entity.eventBroker = eventBroker
		addEntityByComponentTypes(entity)
		if (entity instanceof PanelEntity) {
			panels << entity
		}
	}

	void addEntityByComponentTypes(GameEntity entity) {
		entity.components.each { component ->
			if (entitiesByComponentType[component.class] == null) {
				entitiesByComponentType[component.class] = []
			}
			entitiesByComponentType[component.class] << entity

			// NOTE: if we're doing this post-init and the bounds are expanded further, we don't currently act on this
			if (component instanceof AreaComponent) {
				if (component.x < minX) minX = component.x
				if (component.y < minY) minY = component.y
				if (component.x + component.width > maxX) maxX = component.x + component.width
				if (component.y + component.height > maxY) maxY = component.y + component.height
			}
		}
	}
	@Subscription
	void componentAdded(ComponentAddedEvent event) {
		if (entitiesByComponentType[event.component.class] == null) {
			entitiesByComponentType[event.component.class] = []
		}
		entitiesByComponentType[event.component.class] << event.entity
		log.debug("Added {} to component lookup list for {}", event.entity, event.component)
		if (event.component instanceof AreaComponent) {
			quadtree.insert(event.entity, event.component)
			log.debug("Added {} to quadtree", event.entity)
		}
	}

	@Subscription
	void componentRemoved(ComponentRemovedEvent event) {
		entitiesByComponentType[event.component.class]?.remove(event.entity)
		log.debug("Removed {} from component lookup list for {}", event.entity, event.component)
	}

	@Subscription
	void entityMoved(EntityMovedEvent event) {
		log.trace "Handling entity moved event: {}", event
		def moved = quadtree.move(event.entity, event.oldPosition, event.newPosition)
		if (!moved) {
			log.error ("FAILED: Moved quadtree location of {} from {} to {} (success: $moved)", event.entity, event.oldPosition, event.newPosition)
			throw new IllegalStateException('Move failed')
		}
		log.debug("Moved quadtree location of {} from {} to {} (success: $moved)", event.entity, event.oldPosition, event.newPosition)
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
			log.trace "Finding {} non-panels within {} took {} ms:\n{}", result.size(), area, System.currentTimeMillis() - start, result
		}
		return result
	}

	/**
	 * @param entity an entity which has been updated such that it needs to be re-rendered
	 */
	void entityNeedsRendering(GameEntity entity, AreaComponent previousArea = null) {
		entitiesToBeRendered << entity
		if (previousArea && previousArea != entity.getComponentOfType(AreaComponent)) {
			// Need to repaint what's at the entity's previous location
			def uncoveredEntities = findNonPanelWithin(previousArea)
			if (uncoveredEntities) {
				entitiesToBeRendered.addAll(uncoveredEntities)
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
