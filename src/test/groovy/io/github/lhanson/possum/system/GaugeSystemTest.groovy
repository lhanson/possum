package io.github.lhanson.possum.system

import io.github.lhanson.possum.entity.GaugeEntity
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.scene.Scene
import spock.lang.Specification

import static io.github.lhanson.possum.scene.SceneBuilder.*

class GaugeSystemTest extends Specification {

	def "Gauge system stores a list of gauges per scene"() {
		given:
			GaugeSystem gaugeSystem = new GaugeSystem(eventBroker: Mock(EventBroker))
			Scene scene1 = createScene({[new GaugeEntity()]})
			Scene scene2 = createScene({[new GaugeEntity()]})
		when:
			gaugeSystem.initScene(scene1)
			gaugeSystem.initScene(scene2)
		then:
			gaugeSystem.gauges[scene1.id].size() == 1
			gaugeSystem.gauges[scene2.id].size() == 1
	}

}
