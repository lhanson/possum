package io.github.lhanson.possum.scene

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GaugeComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.PanelEntity
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
			def gaugedPanel = new PanelEntity(
					name: 'testPanel',
					components: [new GaugeComponent(), new GaugeComponent()])
			Scene scene = new Scene('testScene', [gaugedPanel])
		when:
			def gaugedEntities = scene.getEntitiesMatching([GaugeComponent])
		then:
			gaugedEntities
	}

	def "findAt works with no matches"() {
		given:
			GameEntity testEntity = new GameEntity(name: 'testEntity')
			Scene scene = new Scene('testId', [testEntity])
		when:
			def result = scene.findWithin(new AreaComponent())
		then:
			result == []
	}

}
