package io.github.lhanson.possum.rendering

import io.github.lhanson.possum.component.VectorComponent
import spock.lang.Specification

class ViewportTest extends Specification {

	def "Center viewport"() {
		given:
			Viewport viewport = new Viewport()

		when:
			viewport.centerOn(new VectorComponent(100, 100))

		then:
			viewport.x == 100 - (viewport.width / 2)
			viewport.y == 100 - (viewport.height / 2)
	}

}
