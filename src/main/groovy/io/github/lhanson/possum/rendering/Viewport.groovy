package io.github.lhanson.possum.rendering

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.VectorComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Viewport extends AreaComponent {
	Logger log = LoggerFactory.getLogger(this.class)

	Viewport() {
		super(0, 0, 100, 40)
	}

	void centerOn(VectorComponent pos) {
		x = pos.x - (width / 2)
		y = pos.y - (height / 2)
		log.debug "Centered viewport at {}", pos
	}

}
