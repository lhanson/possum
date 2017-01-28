package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.GaugeComponent
import io.github.lhanson.possum.entity.GameEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GaugeSystem implements GameSystem {
	private Logger log = LoggerFactory.getLogger(this.class)
	String name = 'GaugeSystem'

	@Override
	void update(List<GameEntity> entities, double elapsed) {
		findGauged(entities).each { entity ->
			entity.gauges.each { it.update(elapsed) }
		}
	}

	List<GaugedEntity> findGauged(List<GameEntity> entities) {
		entities.findResults { entity ->
			def gauges = entity.components.findAll { it instanceof GaugeComponent }
			if (gauges) {
				return new GaugedEntity(name: entity.name, gauges: gauges, components: entity.components)
			}
		}
	}

	class GaugedEntity implements GameEntity {
		String name = 'GaugedEntity'
		List<GameComponent> components
		List<GaugeComponent> gauges
	}
}
