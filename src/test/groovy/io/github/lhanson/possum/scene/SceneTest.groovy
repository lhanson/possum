package io.github.lhanson.possum.scene

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.RelativeWidthComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GaugeEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.TextEntity
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.ComponentRemovedEvent
import io.github.lhanson.possum.events.EntityPreRenderEvent
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.events.Subscription
import spock.lang.Specification

import static io.github.lhanson.possum.scene.SceneBuilder.createScene

class SceneTest extends Specification {

	def "Find entity by component"() {
		given:
			def entity = new GameEntity(
					name: 'testEntity',
					components: [new AreaComponent()])
			Scene scene = createScene({[entity]})
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
			Scene scene = createScene({[new GameEntity(components: [new AreaComponent()])]})
		when:
			scene.uninit()
		then:
			!scene.initialized
			scene.entities.empty
			scene.getEntitiesMatching([AreaComponent]).empty
			scene.eventBroker.subscriptionsByEventClass.each {
				assert it.value.empty
			}
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
			def menuTitle = new TextEntity('Main Menu',
					new RelativePositionComponent(50, 50))
			def pressStart = new TextEntity('-- press [enter] to start, [esc] to quit --',
					new RelativePositionComponent( 50, 90))
			Scene scene = new Scene('testId', {[menuTitle, pressStart]})
			scene.eventBroker = new EventBroker()
			scene.init()
			scene.entitiesToBeRendered.clear() // get rid of the RerenderEntity added by init

		when:
			[menuTitle, pressStart].each { scene.entityNeedsRendering(it) }

		then:
			scene.entitiesToBeRendered.size() == 2
	}

	def "Scene will report whether an entity is queued for rendering"() {
		given:
			GameEntity entity = new GameEntity()
			Scene scene = new Scene('testId', {[entity]})
			scene.eventBroker = new EventBroker()
			scene.init()

		when:
			scene.entityNeedsRendering(entity)

		then:
			scene.queuedForRendering(entity)
			!scene.queuedForRendering(new GameEntity())
	}

	def "adding an area component triggers addition to the quadtree"() {
		given:
			def entity = new GameEntity()
			Scene scene = new Scene('testId', {[entity]})
			scene.eventBroker = new EventBroker()
			scene.init()

		when:
			entity.components << new AreaComponent(0, 0, 1, 1)

		then:
			scene.quadtree.getAll() == [entity]
	}

	def "UI panels are not included in the quadtree"() {
		given:
			def panel = new PanelEntity()
			Scene scene = new Scene('testId', {[panel]})
			scene.eventBroker = new EventBroker()
			scene.init()

		when:
			panel.components.add(new AreaComponent(0, 0, 1, 1))

		then:
			scene.quadtree.getAll() == []
	}

	def "UI panel contents do not get added to quadtree via component added events"() {
		given:
			TextEntity text = new TextEntity('MAIN MENU')
			PanelEntity panel = new PanelEntity(text)
			Scene scene = new Scene('id', {[panel]})
			scene.eventBroker = new EventBroker()
		when:
			scene.init()
			// Adding an area to a component causes quadtree addition in most cases
			text.components.add(new AreaComponent(10, 1, 1, 1))
		then:
			scene.quadtree.getAll() == []
	}

	def "getEntitiesMatching resolves entities contained in others' inventories"() {
		given:
			Scene menuScene = new Scene('menu', {
				def menuPanel = new PanelEntity(name: 'menu')
				def menuText = new TextEntity('MAIN MENU',
						new RelativePositionComponent(50, 50))
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

	def "Resolve relative positioning by adding a concrete AreaComponent"() {
		given:
			def panel = new PanelEntity(components: [new RelativePositionComponent(50, 50)])
			Scene scene = createScene({[panel]})
		when:
			scene.resolveRelativePositions()
			AreaComponent panelArea = panel.getComponentOfType(AreaComponent)
		then:
			panelArea.height == 2 // At minimum it's a top border and a bottom border
			panelArea.width == panel.padding * 2
			panelArea.x == (scene.viewport.width * 0.5) - (panelArea.width / 2)
			panelArea.y == (scene.viewport.height * 0.5) - (panelArea.height / 2)
	}

	def "Resolve relative position of inventory elements within parent panel"() {
		given:
			def menuItem = new TextEntity(components: [
					new TextComponent('Menu text'),
					new RelativePositionComponent(50, 50),
			])
			def panel = new PanelEntity(components: [
					new AreaComponent(100, 100, 0, 40, 40),
					new InventoryComponent([menuItem])
			])
		when: 'Scene initializer resolves relative positions'
			createScene({[panel]})
		then:
			AreaComponent panelArea = panel.getComponentOfType(AreaComponent)
			AreaComponent menuItemArea = menuItem.getComponentOfType(AreaComponent)

			// Computed area is still relative to parent, viewport resolution happens on render
			menuItemArea.x == Math.floor((panelArea.width * 0.5) - (menuItem.text.size() / 2))
			menuItemArea.y == 1
	}

	def "Resolve panels' inventory elements with padding taken into account"() {
		given:
			def menuItem = new TextEntity('Menu text')
			def panel = new PanelEntity(
					padding: 10,
					components: [ new InventoryComponent([menuItem]) ])
		when: 'Scene initializer resolves panel item positions'
			createScene({[panel]})
		then:
			AreaComponent menuItemArea = menuItem.getComponentOfType(AreaComponent)
			menuItemArea.x == 10
			menuItemArea.y == 10
	}

	def "Scene initializer resolves relative widths"() {
		given:
			def panelEntity = new PanelEntity(components: [new RelativeWidthComponent(50)])
			def textEntity = new TextEntity('test text')
			panelEntity.components.add(new InventoryComponent([textEntity]))
		when:
			Scene scene = createScene({[panelEntity]})
		then:
			AreaComponent panelArea = panelEntity.getComponentOfType(AreaComponent)
			panelArea.width == scene.viewport.width / 2
	}

	def "Correct placement of panel with relative position and width with padding"() {
		given:
			def simulationHzGauge = new GaugeEntity(name: 'simulationHzGauge', components: [new TextComponent(' ')])
			def fpsGauge = new GaugeEntity(name: 'fpsGauge', components: [new TextComponent('')])
			PanelEntity panel = new PanelEntity(name: 'rightHudPanel', padding: 1, components: [
					new RelativePositionComponent(100, 100),
					new RelativeWidthComponent(20),
					new InventoryComponent([simulationHzGauge, fpsGauge])
			])

		when: 'Scene initializer resolves panel position'
			Scene scene = createScene({[panel]})
		then: 'The panel is fully within the viewport'
			AreaComponent ac = panel.getComponentOfType(AreaComponent)
			ac.x == scene.viewport.width - ac.width
			ac.y == scene.viewport.height - ac.height
			ac.x + ac.width == scene.viewport.width
			ac.y + ac.height == scene.viewport.height
	}

	def "Each entity is wired with a reference to its scene"() {
		when:
			GameEntity entity = new GameEntity()
			Scene scene = createScene({[entity]})
		then:
			entity.scene == scene
	}

	def "Scene notifies of EntityPreRenderEvent before adding entity to render queue"() {
		boolean queuedBeforeNotified = false
		given: 'A subscription to EntityPreRenderEvents'
			GameEntity entity = new GameEntity()
			Scene scene = createScene({[entity]})
			def subscriber = new Object() {
				@Subscription
				void notify(EntityPreRenderEvent event) {
					queuedBeforeNotified = scene.queuedForRendering(entity)
				}
			}
			scene.eventBroker.subscribe(subscriber)
		when: 'An entity pre-render notification is sent'
			scene.entityNeedsRendering(entity)
		then: 'It is sent before the entity is added to the render queue'
			!queuedBeforeNotified
	}

}
