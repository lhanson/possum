package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.TimerComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.scene.Scene
import spock.lang.Specification

import static io.github.lhanson.possum.scene.SceneBuilder.*

class TimerSystemTest extends Specification {

	def "Timer system stores timers by scene"() {
		given:
			TimerSystem timerSystem = new TimerSystem(eventBroker: Mock(EventBroker))
			Scene scene1 = createScene({[new GameEntity(components: [new TimerComponent()])]})
			Scene scene2 = createScene({[new GameEntity(components: [new TimerComponent()])]})
		when:
			timerSystem.initScene(scene1)
			timerSystem.initScene(scene2)
		then:
			timerSystem.timers[scene1.id].size() == 1
			timerSystem.timers[scene2.id].size() == 1
	}

}
