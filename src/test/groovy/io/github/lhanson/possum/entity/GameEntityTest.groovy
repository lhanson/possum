package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.CameraFocusComponent
import io.github.lhanson.possum.collision.ImpassableComponent
import io.github.lhanson.possum.component.TextComponent
import spock.lang.Specification

class GameEntityTest extends Specification {

	def "setComponents replaces existing list"() {
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
			entity.getComponents() == newComponents
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

	def "addComponent updates map of components by type"() {
		given:
			def entity = new GameEntity(
					name: 'testEntity',
					components: [ new CameraFocusComponent() ])
			TextComponent textComponent = new TextComponent()

		when:
			entity.addComponent(textComponent)
			def results = entity.getComponentsOfType(TextComponent)

		then:
			results == [textComponent]
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

}
