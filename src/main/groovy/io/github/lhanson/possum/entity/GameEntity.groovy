package io.github.lhanson.possum.entity

import groovy.transform.Sortable
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.ComponentRemovedEvent
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.scene.Scene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * An entity is a general-purpose game object. It possess various
 * {@link GameComponent}s which describe its capabilities and state.
 * {@code GameEntity} behavior is processed by {@link io.github.lhanson.possum.system.GameSystem}s.
 */
@Sortable(includes = ['name', 'id'])
class GameEntity {
	static int nextId = 0
	Logger logger = LoggerFactory.getLogger(this.class)

	/** Used to easily differentiate object equality */
	int id
	/** The name of the entity */
	String name
	/** The scene the entity belongs to */
	Scene scene

	static boolean addInternal = true
	/** The {@link GameComponent}s describing this entity's properties */
	List<GameComponent> components = new ArrayList() {
		@Override boolean add(Object gc) {
			boolean added = true
			// Intercept attempts to add to the raw collection
			if (GameEntity.addInternal) {
				added = addComponentInternal(gc, true)
			}
			if (added) {
				super.add(gc)
			}
		}
		@Override boolean addAll(Collection c) {
			c.each { add(it) }
		}
	}
	/** Map used for internal lookups of components by type without iterating each time */
	Map<Class, List<GameComponent>> componentsByType = [:]
	/** The entity this belongs to, if any. A panel, for example. **/
	GameEntity parent = null
	/** Broker for publishing events */
	EventBroker eventBroker
	/** Whether this entity has been completely initialized */
	boolean initialized = false

	GameEntity() {
		id = nextId++
	}

	void init() {
		initialized = true
	}

	void setEventBroker(EventBroker eventBroker) {
		this.eventBroker = eventBroker
		// All entities are scanned for subscription annotations
		eventBroker?.subscribe(this)
	}

	/**
	 * @return the components associated with this entity
	 */
	List<GameComponent> getComponents() {
		return components
	}

	void setComponents(List<GameComponent> components) {
		this.components.clear()
		componentsByType.clear()
		try {
			// Disable the intercepted call to addComponentInternal
			// since we're explicitly calling it here
			addInternal = false
			for (GameComponent component : components) {
				this.components << component
				addComponentInternal(component, false)
			}
		} finally {
			addInternal = true
		}
	}

	/**
	 * Used internally by our overridden components.add()
	 *
	 * The ArrayList superclass will handle adding the component
	 * to the collection, here we handle doing the additional
	 * lookup table housekeeping, and event notification
	 *
	 * @param component the component being added to this entity
	 * @param publishEvent whether to publish a ComponentAddedEvent; generally true unless
	 *        we're being called from setComponents which happens before initialization is complete
	 * @return whether the component was added to our components collection
	 */
	private boolean addComponentInternal(GameComponent component, boolean publishEvent) {
		boolean added = true
		if (component instanceof InventoryComponent) {
			if (!component.is(this?.inventoryComponent)) {
				// An inventory component is automatically created in the constructor, don't add another one
				logger.debug "Entity {} already has an inventory, copying new entities to existing one", name
				this?.inventoryComponent.inventory.addAll(component.inventory)
				added = false
			}
			// Create link to the inventory items' parent
			component.inventory.each {
				it.parent = this
			}
		}

		if (added) {
			// Map it by its class,
			def classes = [component.class]
			// every Possum interface it implements,
			classes.addAll(component.class.interfaces.findAll { it.name.startsWith 'io.github.lhanson.possum.'})
			// or every Possum class it extends
			Class superclass = component.class.superclass
			while (superclass.name.startsWith('io.github.lhanson.possum.')) {
				classes << superclass
				classes.addAll(superclass.interfaces.findAll { it.name.startsWith 'io.github.lhanson.possum.'})
				superclass = superclass.superclass
			}
			classes.each { Class clazz ->
				logger.debug "'$name' registering component '$component' as '$clazz'"
				if (componentsByType[clazz] == null) {
					componentsByType[clazz] = []
				}
				componentsByType[clazz] << component
			}
			if (publishEvent) {
				eventBroker?.publish(new ComponentAddedEvent(this, component))
			}
		}
		return added
	}

	/**
	 * Removes the component from the entity as well as the
	 * componentsByType lookup map.
	 * @param component the component to remove from this entity
	 */
	void removeComponent(GameComponent component) {
		components.remove(component)
		componentsByType[component.class]?.remove(component)
		eventBroker?.publish(new ComponentRemovedEvent(this, component))
	}

	/**
	 * Returns all components belonging to this entity of the provided type
	 * @param requiredType the Class of components to be returned
	 * @return all components belonging to the entity which are instances of {@code requiredType}
	 */
	List<GameComponent> getComponentsOfType(Class requiredType) {
		componentsByType[requiredType]
	}

	/**
	 * Returns the first component belonging to this entity of the provided type
	 * @param requiredType the Class of components to be returned
	 * @return the first component belonging to the entity which is an instance of {@code requiredType}
	 */
	GameComponent getComponentOfType(Class requiredType) {
		getComponentsOfType(requiredType)?.get(0)
	}

	@Override
	String toString() {
		(name ?: this.class.name) + ": " + getComponentOfType(AreaComponent)
	}
}
