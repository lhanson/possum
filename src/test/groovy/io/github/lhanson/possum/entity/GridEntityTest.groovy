package io.github.lhanson.possum.entity

import spock.lang.Specification

class GridEntityTest extends Specification {

	def "Grid entity creation"() {
		when:
			def grid = new GridEntity(10, 9)
		then:
			grid.width == 10
			grid.height == 9
			grid.cells.size() == grid.width
			grid.cells[0].size() == grid.height
	}

}
