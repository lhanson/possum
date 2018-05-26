package io.github.lhanson.possum.component.layout

import spock.lang.Specification

class PaddingComponentTest extends Specification {

	def "Single-parameter constructor assigns one value to all sides"() {
		when:
			PaddingComponent padding = new PaddingComponent(5)
		then:
			padding.top == 5
			padding.right == 5
			padding.bottom == 5
			padding.left == 5
	}

	def "Width is computed"() {
		given:
			PaddingComponent padding = new PaddingComponent(1, 2, 3, 4)
		expect:
			padding.width == padding.left + padding.right
	}

	def "Height is computed"() {
		given:
			PaddingComponent padding = new PaddingComponent(1, 2, 3, 4)
		expect:
			padding.height == padding.top + padding.bottom
	}

}
