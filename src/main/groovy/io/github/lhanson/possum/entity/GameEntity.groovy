package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.GameComponent

/**
 * An entity is a general-purpose game object. It possess various
 * {@link GameComponent}s which describe its capabilities and state.
 * {@code GameEntity} behavior is processed by {@link io.github.lhanson.possum.system.GameSystem}s.
 */
class GameEntity {
	/** The name of the entity */
	String name
	/** The {@link GameComponent}s describing this entity's properties */
	List<GameComponent> components = []
	/** Map used for internal lookups of components by type without iterating each time */
	Map<Class, List<GameComponent>> componentsByType = [:]
	/** The entity this belongs to, if any. A panel, for example. **/
	GameEntity parent = null

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

	/**
	 * After the entity's list of components has changed (for example, when
	 * resolving relative positioning components and adding concrete locations),
	 * this method will update the lookup map of components by type
	 */
	void updateComponentLookupCache() {
		componentsByType.clear()
		components.each {
			if (componentsByType[it.class] == null) {
				componentsByType[it.class] = []
			}
			componentsByType[it.class] << it
		}
	}

	@Override
	String toString() {
		name ?: this.class.name
	}
}
