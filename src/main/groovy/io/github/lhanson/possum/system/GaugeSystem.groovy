package io.github.lhanson.possum.system

import io.github.lhanson.possum.entity.GaugeEntity
import io.github.lhanson.possum.scene.Scene
import org.springframework.stereotype.Component

@Component
class GaugeSystem extends GameSystem {
	String name = 'GaugeSystem'

	@Override
	void doUpdate(Scene scene, double elapsed) {
		scene.gauges.each { GaugeEntity gauge ->
			def before = gauge.text
			gauge.update(elapsed)
			if (gauge.text != before) {
				log.trace "Gauge {} value changed", gauge.name
				scene.entityNeedsRendering(gauge)
			}
		}
	}
}
