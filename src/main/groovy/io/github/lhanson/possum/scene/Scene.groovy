package io.github.lhanson.possum.scene

import io.github.lhanson.possum.component.PositionComponent
import io.github.lhanson.possum.component.VelocityComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.MobileEntity
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
	Map<Class, List<GameEntity>> entitiesByEntityType = [:]

	/** Unique identifier for this scene */
	String id
	/** Game entities active in this scene */
	List<GameEntity> entities = []
	/** Input contexts for this scene */
	List<InputContext> inputContexts = []
	/** The input collected for this scene to process */
	Set<MappedInput> activeInput = []

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
				if (value == componentTypes.size()) {
					matches << key
				}
			}
		}
		return matches
	}

	/**
	 * @return all movable {@link GameEntity} objects conveniently packaged as {@link MobileEntity}
	 */
	List<MobileEntity> getMobileEntities() {
		def result = entitiesByEntityType[MobileEntity]
		if (result == null) {
			result = getEntitiesMatching([PositionComponent, VelocityComponent]).collect { new MobileEntity(it) }
			entitiesByEntityType[MobileEntity] = result
		}
		result
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
