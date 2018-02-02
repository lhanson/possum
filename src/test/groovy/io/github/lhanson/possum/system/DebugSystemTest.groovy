package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.scene.Scene
import io.github.lhanson.possum.scene.SceneBuilder
import spock.lang.Specification

class DebugSystemTest extends Specification {
	DebugSystem debugSystem
	Scene scene

	def setup() {
		debugSystem = new DebugSystem(eventBroker: Mock(EventBroker))
		scene = SceneBuilder.createScene()
		debugSystem.doInitScene(scene)
	}

	def "Debug mode for a scene begins disabled"() {
		expect:
			!debugSystem.debugEnabled.contains(scene.id)
	}

	def "Debug status can be toggled"() {
		given: 'DEBUG input is active'
			scene.activeInput << MappedInput.DEBUG
		when: 'Scene is updated'
			debugSystem.doUpdate(scene, 0)
		then: 'The debug flag for the scene is toggled true'
			debugSystem.debugEnabled.contains(scene.id)

		when: 'Another update with the DEBUG input present'
			debugSystem.doUpdate(scene, 0)
		then: 'Debug flag is toggled back to false'
			!debugSystem.debugEnabled.contains(scene.id)
	}

	def "Debug status is stored per scene"() {
		given:
			Scene scene2 = SceneBuilder.createScene()
			debugSystem.doInitScene(scene2)
		when:
			scene2.activeInput << MappedInput.DEBUG
			debugSystem.doUpdate(scene2, 0)
		then:
			!debugSystem.debugEnabled.contains(scene.id)
			debugSystem.debugEnabled.contains(scene2.id)
	}

	def "Quadtree outline entities are stored per scene"() {
		given: 'Populate the quadtree initially'
			GameEntity entity = new GameEntity(components: [new AreaComponent(10, 10, 1, 1)])
			Scene scene2 = SceneBuilder.createScene({[entity]})
			debugSystem.doInitScene(scene2)
			scene2.activeInput << MappedInput.DEBUG
		when: 'Update the debugged scene to create debug entities'
			debugSystem.doUpdate(scene2, 0)
		then: 'Debug entities are stored for that scene only'
			debugSystem.debugEntities[scene.id].empty
			debugSystem.debugEntities[scene2.id]

		when: 'Toggling debug mode off'
			debugSystem.doUpdate(scene2, 0)
		then: 'Debug entities are removed'
			debugSystem.debugEntities[scene.id].empty
			debugSystem.debugEntities[scene2.id].empty
	}

	def "Render hints for a given entity are added only per frame"() {
		given: 'A scene in debug mode with an entity ready to render'
			GameEntity entity = new GameEntity(components: [new AreaComponent(10, 10, 1, 1)])
			scene.eventBroker.subscribe(debugSystem)
			scene.addEntity(entity)
			debugSystem.debugEnabled.add(scene.id)

		when: 'The scene is informed that the entity needs rendering'
			scene.entityNeedsRendering(entity)

		then: 'The debug system responds by creating a render hint'
			scene.debugEntitiesToBeRendered.size() == 1

		when: 'The entity is passed in again during the same frame'
			scene.entityNeedsRendering(entity)
		then: 'Subsequent debug hints are not created'
			scene.debugEntitiesToBeRendered.size() == 1
	}

}
