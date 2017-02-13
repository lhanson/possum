package io.github.lhanson.possum.component

import spock.lang.Specification

class AreaComponentTest extends Specification {

	def "Same area overlaps"() {
		when:
			def area1 = new AreaComponent(0, 0, 1, 1)
			def area2 = new AreaComponent(0, 0, 1, 1)
		then:
			area1.overlaps(area2)
	}

	def "No overlap"() {
		when:
			def area1 = new AreaComponent(0, 0, 1, 1)
			def area2 = new AreaComponent(10, 10, 1, 1)
		then:
			!area1.overlaps(area2)
	}

	def "Adjacent but no overlap"() {
		when:
			def panel = new AreaComponent(0, 0, 1, 1)
			def entity = new AreaComponent(0, 1, 1, 1)
		then:
			!entity.overlaps(panel)
	}

	def "Overlap horizontally"() {
		when:
			def area1 = new AreaComponent(0, 0, 5, 1)
			def area2 = new AreaComponent(-3, 0, 11, 1)
		then:
			area1.overlaps(area2)
	}

}
