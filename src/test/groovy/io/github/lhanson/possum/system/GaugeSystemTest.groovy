package io.github.lhanson.possum.system

import io.github.lhanson.possum.entity.GaugeEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.scene.Scene
import spock.lang.Specification

import static io.github.lhanson.possum.scene.SceneBuilder.*

class GaugeSystemTest extends Specification {
	GaugeSystem gaugeSystem

	def setup() {
		gaugeSystem = new GaugeSystem(eventBroker: Mock(EventBroker))
	}

	def "Gauge system stores a list of gauges per scene"() {
		given:
			Scene scene1 = createScene({[new GaugeEntity()]})
			Scene scene2 = createScene({[new GaugeEntity()]})
		when:
			gaugeSystem.initScene(scene1)
			gaugeSystem.initScene(scene2)
		then:
			gaugeSystem.gauges[scene1.id].size() == 1
			gaugeSystem.gauges[scene2.id].size() == 1
	}

	def "Gauge system finds gauges inside entities' inventories"() {
		given:
			GaugeEntity gauge = new GaugeEntity(name: 'gauge1')
			PanelEntity panel = new PanelEntity()
			panel.inventory.add(gauge)
			Scene scene = createScene({[ panel ]})
		when:
			gaugeSystem.initScene(scene)
		then:
			panel.inventory.contains(gauge)
			gaugeSystem.gauges[scene.id].size() == 1
	}

}
