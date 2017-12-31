package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
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
class GameEntity {
	Logger logger = LoggerFactory.getLogger(this.class)

	/** The name of the entity */
	String name

	/** The {@link GameComponent}s describing this entity's properties */
	private List<GameComponent> components = []
	/** Map used for internal lookups of components by type without iterating each time */
	Map<Class, List<GameComponent>> componentsByType = [:]

	/** The entity this belongs to, if any. A panel, for example. **/
	GameEntity parent = null

	/** Broker for publishing events */
	EventBroker eventBroker

	/**
	 * @return the components associated with this entity
	 */
	List<GameComponent> getComponents() {
		return components
	}

	void setComponents(List<GameComponent> components) {
		this.components = components
		componentsByType.clear()
		components.each {
			if (componentsByType[it.class] == null) {
				componentsByType[it.class] = []
			}
			componentsByType[it.class] << it
		}
	}

	/**
	 * Adds the component to the entity and inserts it into the
	 * componentsByType lookup map as well.
	 * @param component the component to add to this entity
	 */
	void addComponent(GameComponent component) {
		components << component
		if (componentsByType[component.class] == null) {
			componentsByType[component.class] = []
		}
		componentsByType[component.class] << component
		eventBroker?.publish(new ComponentAddedEvent(this, component))
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
