package io.github.lhanson.possum.scene

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.RelativePositionComponent
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
			def textPanel = new PanelEntity(components: [new TextComponent(), new TextComponent()])
			Scene scene = new Scene('testScene', {[textPanel]})
			scene.eventBroker = new EventBroker()
			scene.init()
		when:
			def gaugedEntities = scene.getEntitiesMatching([TextComponent])
		then:
			gaugedEntities
	}

	def "setEntities clears existing entries in various collections"() {
		given:
			GameEntity testEntity = new GameEntity(name: 'testEntity', components: [new TextComponent()])
			Scene scene = new Scene('testId', {[testEntity, new PanelEntity()]})
			scene.eventBroker = new EventBroker()
			scene.init()
		when:
			scene.setEntities([])
		then:
			scene.getEntitiesMatching([TextComponent]).empty
			scene.panels.empty
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

	def "The scene initializes its entities"() {
		given:
			GameEntity e1 = new GameEntity()
			GameEntity e2 = new GameEntity()
			Scene scene = new Scene('testId', {[e1, e2]})
			scene.eventBroker = new EventBroker()

		when:
			scene.init()

		then:
			e1.initialized
			e2.initialized
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
			scene.quadtree.countEntities() == 0
	}

	def "Loading scene is uninitialized with parent scene"() {
		given:
			EventBroker eventBroker = new EventBroker()
			Scene loadingScene = new Scene('loading')
			Scene mainScene = new Scene('scene', {}, [], loadingScene)
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

	def "Add entity"() {
		given:
			Scene scene = new Scene('testId')
			GameEntity hero = new GameEntity(name: 'hero')
		when:
			scene.addEntity(hero)
		then:
			scene.entities.size() == 1
			scene.entities.contains(hero)
	}

	def "entitiesToBeRendered is sorted in ascending z-order"() {
		given:
			def entities = []
			def rand = new Random()
			100.times {
				int z = rand.nextInt(1000)
				entities << new GameEntity(name: 'ceiling', components: [new AreaComponent(0, 0, z, 1, 1)])
			}
			Scene scene = new Scene('testId')
			scene.eventBroker = new EventBroker()
			scene.init()
			scene.entitiesToBeRendered.clear() // get rid of the RerenderEntity added by init

		when:
			entities.each { scene.entityNeedsRendering(it) }

		then:
			int previousZ = Integer.MIN_VALUE
			scene.entitiesToBeRendered.each { GameEntity entity ->
				AreaComponent ac = entity.getComponentOfType(AreaComponent)
				assert ac.position.z >= previousZ
				previousZ = ac.position.z
			}
	}

	def "entitiesToBeRendered implements equality correctly"() {
		given:
			def menuTitle = new TextEntity(
					name: 'menuTitle',
					components: [
							new TextComponent('Main Menu'),
							new RelativePositionComponent(50, 50)
					])
			def pressStart = new TextEntity(
					name: 'pressStart',
					components: [
							new TextComponent('-- press [enter] to start, [esc] to quit --'),
							new RelativePositionComponent( 50, 90)
					])
			Scene scene = new Scene('testId', {[menuTitle, pressStart]})
			scene.eventBroker = new EventBroker()
			[scene, menuTitle, pressStart].each { it.init() }
			scene.entitiesToBeRendered.clear() // get rid of the RerenderEntity added by init

		when:
			[menuTitle, pressStart].each { scene.entityNeedsRendering(it) }

		then:
			scene.entitiesToBeRendered.size() == 2
	}

	def "adding an area component triggers addition to the quadtree"() {
		given:
			def entity = new GameEntity()
			Scene scene = new Scene('testId', {[entity]})
			scene.eventBroker = new EventBroker()
			[scene, entity].each { it.init() }

		when:
			// Simulate resolution of relative positions into world coordinates
			entity.components << new AreaComponent(0, 0, 1, 1)

		then:
			scene.quadtree.getAll() == [entity]
	}

	def "UI panels are not included in the quadtree"() {
		given:
			def panel = new PanelEntity()
			Scene scene = new Scene('testId', {[panel]})
			scene.eventBroker = new EventBroker()
			[panel, scene].each { it.init() }

		when:
			// Simulate resolution of relative positions into world coordinates
			panel.components.add(new AreaComponent(0, 0, 1, 1))

		then:
			scene.quadtree.getAll() == []
	}

	def "getEntitiesMatching resolves entities contained in others' inventories"() {
		given:
			Scene menuScene = new Scene('menu', {
				def menuPanel = new PanelEntity(name: 'menu')
				def menuText = new TextEntity(name: 'menuText', parent: menuPanel,
						components: [new TextComponent('MAIN MENU'),
						             new RelativePositionComponent(50, 50)])
				menuPanel.components.add(new InventoryComponent([menuText]))
				[menuPanel]
			})
			menuScene.eventBroker = new EventBroker()
			menuScene.init()

		when:
			def relativeEntities = menuScene.getEntitiesMatching([RelativePositionComponent])

		then:
			relativeEntities.size() == 1
	}

}
