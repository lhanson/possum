package io.github.lhanson.possum.scene

import groovy.transform.ToString
import io.github.lhanson.possum.collision.Quadtree
import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.RerenderEntity
import io.github.lhanson.possum.events.*
import io.github.lhanson.possum.input.InputAdapter
import io.github.lhanson.possum.input.InputContext
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.rendering.Viewport
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static io.github.lhanson.possum.component.AreaComponent.FrameOfReference.ASCII_PANEL

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
	/**
	 * The area of the world visible to the player on screen.
	 * Each scene maintains its own world and viewport.
	 */
	Viewport viewport
	/** Event broker for this scene */
	EventBroker eventBroker
	/** Whether the scene has been initialized yet */
	boolean initialized = false

	List<GameEntity> panels = []
	private Logger log = LoggerFactory.getLogger(this.class)
	// Initialize (or reinitialize) the scene
	private SceneInitializer sceneInitializer
	// Top-level entities active in this scene, does not include entities in inventories
	private List<GameEntity> entities = []
	// Input contexts for this scene
	private List<InputContext> inputContexts = []
	private Map<Class, List<GameEntity>> entitiesByComponentType = [:]
	private Comparator zOrderComparator = { GameEntity a, GameEntity b ->
		AreaComponent acA = a.getComponentOfType(AreaComponent)
		AreaComponent acB = b.getComponentOfType(AreaComponent)
		// Order by z-axis if they differ, otherwise use natural object comparator
		return acA?.position?.z <=> acB?.position?.z ?: a <=> b
	}
	// A set of entities modified in such a way as to require re-rendering
	private SortedSet<GameEntity> entitiesToBeRendered = new TreeSet(zOrderComparator)
	// A set of entities representing rendering hints when in debug mode
	private SortedSet<GameEntity> debugEntitiesToBeRendered = new TreeSet(zOrderComparator)
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

		viewport = new Viewport()

		if (!eventBroker) throw new IllegalStateException("No event broker for scene $id")
		eventBroker.subscribe(this)

		setEntities(sceneInitializer?.initScene())

		resolveRelativePositions()

		// Initialize quadtree
		quadtree = new Quadtree(new AreaComponent(minX, minY, maxX, maxY))
		if (entitiesByComponentType[AreaComponent]) {
			def worldEntities = entitiesByComponentType[AreaComponent].grep {
				!(it instanceof PanelEntity) && !it.parent
			}
			log.debug "Initializing quadtree with ${worldEntities.size()} area entities with bounds [$minX, $minY] to [$maxX, $maxY], total grid space of ${(maxX - minX) * (maxY - minY)} locations"
			long start = System.currentTimeMillis()
			worldEntities.each { quadtree.insert it }
			log.debug("Initialized quadtree in {} ms", System.currentTimeMillis() - start)
		}

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
		quadtree.clear()
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
		if (!entity.initialized) {
			entity.init()
		}
		entities << entity
		entity.eventBroker = eventBroker
		entity.scene = this
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

			if (component instanceof InventoryComponent) {
				component.inventory.each { addEntity(it) }
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
		if (event.component instanceof AreaComponent &&
				!(event.entity instanceof PanelEntity) &&
				(!event.entity.parent || !(event.entity.parent instanceof PanelEntity))) {
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
		log.trace "Scene $id handling entity moved event: {}", event
		def moved = quadtree.move(event.entity, event.oldPosition, event.newPosition)
		if (!moved) {
			if (!quadtree.getAll().contains(event.entity)) {
				log.error ("Quadtree does not contain this entity (${event.entity})")
			}
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
	 * Will recursively search Inventory components as well.
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
		def entities = [entity]
		AreaComponent area = entity.getComponentOfType(AreaComponent)
		if (previousArea && previousArea != area) {
			if (entity.parent) {
				// Rerender background of the parent if applicable
				AreaComponent pc = entity.parent.getComponentOfType(AreaComponent)
				AreaComponent newArea = translateChildToParent(area, pc)
				AreaComponent oldArea = translateChildToParent(previousArea, pc)
				oldArea.subtract(newArea).each {
					it.frameOfReference = ASCII_PANEL
					log.debug "Adding render task; background for previous location of entity '${entity.name}' at $it"
					entities << new RerenderEntity(name: "Background for ${entity.name}", components: [it], scene: this)
				}
			} else {
				// Need to repaint what's at the entity's previous location
				def uncoveredEntities = findNonPanelWithin(previousArea)
				if (uncoveredEntities) {
					log.debug "Adding render task for uncovered entities: {}", uncoveredEntities
					entitiesToBeRendered.addAll(uncoveredEntities)
				}
			}
		}

		entities.each { it ->
			// We publish the event before adding the entity to the queue so that
			// listeners can check for its presence on the queue as a sign that
			// they've already processed updates to this entity, because systems
			// can be updated more than once before a render is done.
			eventBroker.publish(new EntityPreRenderEvent(it, previousArea))
			entitiesToBeRendered << it
		}
	}

	/**
	 * Queues the given entity as a debug hint.
	 * @param entity a render debug hint
	 */
	void debugEntityNeedsRendering(GameEntity entity) {
		debugEntitiesToBeRendered << entity
	}

	boolean queuedForRendering(GameEntity entity) {
		entitiesToBeRendered.contains(entity)
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

	/**
	 * Finds relatively-positioned entities within the scene and assigns them concrete
	 * positions relative to their enclosing areas (whether that's the viewport itself
	 * or panels they're contained in).
	 */
	void resolveRelativePositions() {
		log.debug "Resolving relatively positioned entities in scene '$id'"
		// Center viewport on focused entity, important that this happens
		// before resolving relatively positioned entities
		GameEntity focusedEntity = entities.find { it.getComponentOfType(CameraFocusComponent) }
		if (focusedEntity) {
			AreaComponent area = focusedEntity.getComponentOfType(AreaComponent)
			if (area) {
				viewport.centerOn(area.position)
			}
		}

		getEntitiesMatching([RelativePositionComponent]).each { GameEntity entity ->
			log.debug "Initializing relatively positioned entity ${entity.name}"

			// Determine parent reference
			AreaComponent parentReference
			if (entity.parent) {
				parentReference = new AreaComponent(entity.parent.getComponentOfType(AreaComponent))
			} else {
				parentReference = new AreaComponent(viewport)
			}

			// See what components are set already
			AreaComponent ac = entity.getComponentOfType(AreaComponent)
			RelativePositionComponent rpc = entity.getComponentOfType(RelativePositionComponent)
			RelativeWidthComponent rwc = entity.getComponentOfType(RelativeWidthComponent)
			boolean newArea = false
			if (!ac) {
				ac = new AreaComponent()
				newArea = true
			}

			if (rwc) {
				// Compute relative width
				ac.width = (rwc.width / 100.0f) * parentReference.width
			}

			if (entity instanceof PanelEntity) {
				ac.x = constrainInt(relativeX(rpc) - (int)(ac.width / 2), 0, parentReference.width - (ac.width))
				ac.y = constrainInt(relativeY(rpc) - (int)(ac.height / 2), 0, parentReference.height - (ac.height))
			} else {
				List<io.github.lhanson.possum.component.TextComponent> text =
						entity.getComponentsOfType(io.github.lhanson.possum.component.TextComponent)
				int xOffset = 0
				if (text?.get(0)) {
					// This is assuming we want to center this text around its position
					xOffset = Math.floor(text[0].text.length() / 2)
					ac.width = text.collect { it.text.size() }.max() // Longest string
					// Assumes each text component is on its own line
					ac.height = text.size()
				}
				ac.x = (rpc.x / 100.0f) * parentReference.width - xOffset
				ac.y = (rpc.y / 100.0f) * parentReference.height
			}

			if (newArea) {
				entity.components << ac
				log.debug "Added area component for {}", entity.name
			}

			log.debug "Calculated position of {} for {}", ac, entity.name
		}
	}

	// Returns value if it falls within the bounds, otherwise the nearest boundary
	int constrainInt(int value, int min, int max) {
		Math.min(Math.max(value, min), max)
	}

	int relativeX(RelativePositionComponent rpc) {
		return (int) ((rpc.x / 100.0f) * viewport.width)
	}

	int relativeY(RelativePositionComponent rpc) {
		return (int) ((rpc.y / 100.0f) * viewport.height)
	}

	/**
	 * Entities positioned relative to a parent need their coordinates added
	 * to those of the parent in order to get rendering coordinates in the parent's
	 * frame of reference.
	 *
	 * @param child the entity positioned relative to a parent
	 * @param parent the entity whose coordinates determine the child's absolute position
	 * @return an area describing the child's screen coordinates in the parent's frame of reference
	 */
	AreaComponent translateChildToParent(AreaComponent child, AreaComponent parent) {
		// child + panel
		new AreaComponent(child.x + parent.x, child.y + parent.y, child.width, child.height)
	}

}
