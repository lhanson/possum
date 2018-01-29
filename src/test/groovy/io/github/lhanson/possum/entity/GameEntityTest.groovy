package io.github.lhanson.possum.entity

import io.github.lhanson.possum.collision.CollisionHandlingComponent
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.CameraFocusComponent
import io.github.lhanson.possum.collision.ImpassableComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.events.Subscription
import spock.lang.Specification

class GameEntityTest extends Specification {

	def "setComponents replaces existing list contents"() {
		given:
			def entity = new GameEntity(components: [new TextComponent()])
			def newComponents = [
					new CameraFocusComponent(),
					new ImpassableComponent(),
					new TextComponent(),
					new TextComponent()
			]

		when:
			entity.setComponents(newComponents)

		then:
			entity.getComponents().size() == newComponents.size()
			entity.getComponents().containsAll(newComponents)
	}

	def "test getComponentsOfType"() {
		given:
			def entity = new GameEntity(
				name: 'testEntity',
				components: [
						new CameraFocusComponent(),
						new ImpassableComponent(),
						new TextComponent(),
						new TextComponent()
				])

		when:
			def results = entity.getComponentsOfType(TextComponent)

		then:
			results.size() == 2
	}

	def "Adding a component updates map of components by type"() {
		given:
			def entity = new GameEntity(
					name: 'testEntity',
					components: [ new CameraFocusComponent() ])
			TextComponent textComponent = new TextComponent()

		when:
			entity.components << textComponent
			def results = entity.getComponentsOfType(TextComponent)

		then:
			results == [textComponent]
	}

	def "Directly adding to component collection is intercepted to update componentsByType"() {
		given:
			def entity = new GameEntity()
			def area = new AreaComponent()
		when:
			entity.components << area
		then:
			entity.componentsByType[AreaComponent] == [area]
	}

	def "Searching for a component by type works for every Possum interface it implements"() {
		when: "An entity has a component implementing multiple Possum interfaces"
			def impassable = new ImpassableComponent()
			def entity = new GameEntity(components: [impassable])

		then: "the component is an instanceof multiple classes"
			impassable instanceof GroovyObject
			impassable instanceof ImpassableComponent
			impassable instanceof CollisionHandlingComponent

		and: "it can be looked up under any of the Possum classes"
			entity.getComponentOfType(ImpassableComponent) == impassable
			entity.getComponentOfType(CollisionHandlingComponent) == impassable

		and: "but we don't index it under non-Possum classes"
			entity.getComponentOfType(GroovyObject) == null
	}

	def "removeComponent updates map of components by type"() {
		given:
			TextComponent textComponent = new TextComponent()
			def entity = new GameEntity(
					name: 'testEntity',
					components: [textComponent])

		when:
			entity.removeComponent(textComponent)
			def results = entity.getComponentsOfType(TextComponent)

		then:
			results == []
	}

	def "removeComponent for nonexistent component doesn't blow up"() {
		given:
			def entity = new GameEntity(components: [])

		when:
			entity.removeComponent(new TextComponent())
			def results = entity.getComponentsOfType(TextComponent)

		then:
			results == null
	}

	def "Setting the components collection doesn't wipe out our add() interceptor"() {
		when:
			GameEntity entity = new PanelEntity(components: [new TextComponent('test text')])
		then:
			entity.getComponentOfType(AreaComponent)
	}

	class SubscribedEntity extends GameEntity {
		@Subscription void subscription(ComponentAddedEvent e) {}
	}
	def "Game entities are scanned for subscription annotations by default"() {
		when:
			GameEntity entity = new SubscribedEntity()
			entity.eventBroker = new EventBroker()
		then:
			entity.eventBroker.subscriptionsByEventClass[ComponentAddedEvent]
	}

	def "Adding inventory component multiple times aggregates contents in a single instance"() {
		given:
			def panel = new PanelEntity(name: 'panel', padding: 10, eventBroker: new EventBroker())
			def panelText = new TextEntity('text')
		when:
			panel.components.add(new InventoryComponent([panelText]))
		then:
			panel.getComponentsOfType(AreaComponent).size() == 1
			panel.getComponentsOfType(InventoryComponent).size() == 1
			panel.getComponentOfType(InventoryComponent).inventory == [panelText]
	}

}
