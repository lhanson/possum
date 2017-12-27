package io.github.lhanson.possum.scene

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.TextEntity
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.ComponentRemovedEvent
import io.github.lhanson.possum.events.EventBroker
import spock.lang.Specification

class SceneTest extends Specification {

	def "Find entity by component"() {
		given:
			def entity = new GameEntity(
					name: 'testEntity',
					components: [new AreaComponent()])
			Scene scene = new Scene('testScene', {[entity]})
			scene.eventBroker = new EventBroker()
			scene.init()
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
			Scene scene = new Scene('testScene', {[textPanel]})
			scene.eventBroker = new EventBroker()
			scene.init()
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
			Scene scene = new Scene('testId', {[testEntity]})
			scene.eventBroker = new EventBroker()
			scene.init()
		when:
			def result = scene.findNonPanelWithin(new AreaComponent())
		then:
			result == []
	}

	def "setEntities clears existing entries in entitiesByComponentType "() {
		given:
			GameEntity testEntity = new GameEntity(name: 'testEntity', components: [TextComponent])
			Scene scene = new Scene('testId', {[testEntity]})
			scene.eventBroker = new EventBroker()
			scene.init()
		when:
			scene.setEntities([])
			def results = scene.getEntitiesMatching([TextComponent])
		then:
			results.isEmpty()
	}

	def "setEntities updates entitiesByComponentType lookup table"() {
		given:
			GameEntity testEntity = new GameEntity(name: 'testEntity', components: [new TextComponent()])
			Scene scene = new Scene('testId', {[testEntity]})
			scene.eventBroker = new EventBroker()
			scene.init()
		when:
			def results = scene.getEntitiesMatching([TextComponent])
		then:
			results == [testEntity]
	}

	def "Component added events are processed"() {
		given:
			GameEntity testEntity = new GameEntity(name: 'testEntity')
			Scene scene = new Scene('testId', {[testEntity]})
			scene.eventBroker = new EventBroker()
			scene.init()
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
			Scene scene = new Scene('testId', {[testEntity]})
			scene.eventBroker = new EventBroker()
			scene.init()
			ComponentRemovedEvent removeEvent = new ComponentRemovedEvent(testEntity, textComponent)

		when:
			scene.eventBroker.publish(removeEvent)
			def results = scene.getEntitiesMatching([TextComponent])

		then:
			results == []
	}

	def "Before initializing"() {
		when:
			Scene scene = new Scene('testId', {[new GameEntity()]})

		then:
			!scene.initialized
			scene.entities.size() == 0
	}

	def "After initializing"() {
		given:
			Scene scene = new Scene('testId', {[new GameEntity()]})
			scene.eventBroker = new EventBroker()

		when:
			scene.init()

		then:
			scene.initialized
			scene.entities.size() == 1
			!scene.eventBroker.subscriptionsByEventClass.empty
	}

	def "Uninitialize"() {
		given:
			Scene scene = new Scene('testId', {
				[new GameEntity(components: [new AreaComponent()])]
			})
			scene.eventBroker = new EventBroker()

		when:
			scene.init()
			scene.uninit()

		then:
			!scene.initialized
			scene.entities.empty
			scene.getEntitiesMatching([AreaComponent]).empty
			scene.eventBroker.subscriptionsByEventClass.findAll { it.value } == [:]
	}

	def "Loading scene is uninitialized with parent scene"() {
		given:
			EventBroker eventBroker = new EventBroker()
			Scene loadingScene = new Scene('loading', {[]})
			Scene mainScene = new Scene('scene', {[]}, [], loadingScene)
			mainScene.eventBroker = eventBroker
			loadingScene.eventBroker = eventBroker

		when:
			// PossumSceneBuilder normally handles the chained initialization
			loadingScene.init()
			mainScene.init()
		and:
			mainScene.uninit()

		then:
			!mainScene.initialized
			!loadingScene.initialized
	}

}
