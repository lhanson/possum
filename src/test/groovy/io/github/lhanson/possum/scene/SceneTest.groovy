package io.github.lhanson.possum.scene

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.TextEntity
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.ComponentRemovedEvent
import spock.lang.Specification

class SceneTest extends Specification {

	def "Find entity by component"() {
		given:
			def entity = new GameEntity(
					name: 'testEntity',
					components: [new AreaComponent()])
			Scene scene = new Scene('testScene', [entity])
		when:
			def entities = scene.getEntitiesMatching([AreaComponent])
		then:
			entities.size() == 1
			entities[0] == entity
	}

	def "Find by component when entity has multiple components of the same type"() {
		given:
			def textPanel = new PanelEntity(
					name: 'textPanel',
					components: [new TextEntity(), new TextEntity()])
			Scene scene = new Scene('testScene', [textPanel])
		when:
			// This is slightly odd in that TextEntity is not technically a GameComponent,
			// but it's still useful to find and there doesn't appear to be a reason to
			// restrict entity matching to GameComponent only.
			def gaugedEntities = scene.getEntitiesMatching([TextEntity])
		then:
			gaugedEntities
	}

	def "findAt works with no matches"() {
		given:
			GameEntity testEntity = new GameEntity(name: 'testEntity')
			Scene scene = new Scene('testId', [testEntity])
		when:
			def result = scene.findNonPanelWithin(new AreaComponent())
		then:
			result == []
	}

	def "setEntities clears existing entries in entitiesByComponentType "() {
		given:
			GameEntity testEntity = new GameEntity(name: 'testEntity', components: [TextComponent])
			Scene scene = new Scene('testId', [testEntity])
		when:
			scene.setEntities([])
			def results = scene.getEntitiesMatching([TextComponent])
		then:
			results.isEmpty()
	}

	def "setEntities updates entitiesByComponentType lookup table"() {
		given:
			GameEntity testEntity = new GameEntity(name: 'testEntity', components: [new TextComponent()])
			Scene scene = new Scene('testId', [testEntity])
		when:
			def results = scene.getEntitiesMatching([TextComponent])
		then:
			results == [testEntity]
	}

	def "Component added events are processed"() {
		given:
			GameEntity testEntity = new GameEntity(name: 'testEntity')
			Scene scene = new Scene('testId', [testEntity])
			ComponentAddedEvent addEvent = new ComponentAddedEvent(testEntity, new TextComponent())

		when:
			scene.eventBroker.publish(addEvent)
			def results = scene.getEntitiesMatching([TextComponent])

		then:
			results == [testEntity]
	}

	def "Component removed events are processed"() {
		given:
			TextComponent textComponent = new TextComponent()
			GameEntity testEntity = new GameEntity(name: 'testEntity', components: [textComponent])
			Scene scene = new Scene('testId', [testEntity])
			ComponentRemovedEvent removeEvent = new ComponentRemovedEvent(testEntity, textComponent)

		when:
			scene.eventBroker.publish(removeEvent)
			def results = scene.getEntitiesMatching([TextComponent])

		then:
			results == []
	}

}
