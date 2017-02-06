package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.FocusedComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.collision.ImpassableComponent
import io.github.lhanson.possum.component.TextComponent
import spock.lang.Specification

class GameEntityTest extends Specification {
	def "test getComponentsOfType"() {
		given:
			def entity = new GameEntity() {
				String name = 'testEntity'
				List<GameComponent> components = [
						new FocusedComponent(),
						new ImpassableComponent(),
						new TextComponent(),
						new TextComponent()
				]
			}

		when:
			def results = entity.getComponentsOfType(TextComponent)

		then:
			results.size() == 2
	}
}
