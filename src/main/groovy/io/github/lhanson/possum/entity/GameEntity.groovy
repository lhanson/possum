package io.github.lhanson.possum.entity

import groovy.transform.Sortable
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.ComponentRemovedEvent
import io.github.lhanson.possum.events.EventBroker
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
	static boolean addInternal = true
	/** The {@link GameComponent}s describing this entity's properties */
	List<GameComponent> components = new ArrayList() {
		@Override boolean add(Object gc) {
			// Intercept attempts to add to the raw collection
			if (GameEntity.addInternal) {
				addComponentInternal(gc, true)
			}
			super.add(gc)
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
	 */
	private void addComponentInternal(GameComponent component, boolean publishEvent) {
		if (component instanceof InventoryComponent) {
			// Create link to the inventory items' parent
			component.inventory.each {
				it.parent = this
			}
		}

		if (componentsByType[component.class] == null) {
			componentsByType[component.class] = []
		}
		componentsByType[component.class] << component
		if (publishEvent) {
			eventBroker?.publish(new ComponentAddedEvent(this, component))
		}
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
		// Check our cached lookup map before searching all components
		def result = componentsByType[requiredType]
		if (result == null) {
			result = components.findAll { requiredType.isInstance(it) }
			componentsByType[requiredType] = result
			logger.trace("getComponentsOfType cache miss. Item was present: {}", !result.isEmpty())
		}
		result
	}

	/**
	 * Returns the first component belonging to this entity of the provided type
	 * @param requiredType the Class of components to be returned
	 * @return the first component belonging to the entity which is an instance of {@code requiredType}
	 */
	GameComponent getComponentOfType(Class requiredType) {
		getComponentsOfType(requiredType)[0]
	}

	@Override
	String toString() {
		(name ?: this.class.name) + ": " + getComponentOfType(AreaComponent)
	}
}
