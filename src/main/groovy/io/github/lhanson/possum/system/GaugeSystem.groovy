package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.GaugeComponent
import io.github.lhanson.possum.scene.Scene
import org.springframework.stereotype.Component

@Component
class GaugeSystem extends GameSystem {
	String name = 'GaugeSystem'

	@Override
	void doUpdate(Scene scene, double elapsed) {
		scene.getEntitiesMatching([GaugeComponent]).each { entity ->
			entity.getComponentsOfType(GaugeComponent).each {
				def before = it.text
				it.update(elapsed)
				if (it.text != before) {
					scene.entityNeedsRendering(entity)
				}
			}
		}
	}
}
