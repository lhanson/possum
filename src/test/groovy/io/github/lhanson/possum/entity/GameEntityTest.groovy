package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.CameraFocusComponent
import io.github.lhanson.possum.collision.ImpassableComponent
import io.github.lhanson.possum.component.TextComponent
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
			results == []
	}

	def "Setting the components collection doesn't wipe out our add() interceptor"() {
		given:
			GameEntity entity = new PanelEntity(components: [new TextComponent('test text')])
		when:
			// Initialization should create a default AreaComponent, inserted with our
			// overridden components.add() method which sets the componentsByType lookup.
			entity.init()
		then:
			entity.getComponentOfType(AreaComponent)
	}

}
