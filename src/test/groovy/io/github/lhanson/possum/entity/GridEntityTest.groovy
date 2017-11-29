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

	def "Cell neighbor linking"() {
		given:
			def grid = new GridEntity(2, 2)
		when:
			def cell1 = grid.cellAt(0, 0)
			def cell2 = grid.cellAt(1, 0)

		then:
			cell1 == cell2.west
			cell1.east == cell2
	}

	def "4-way cell neighborhood linking"() {
		given:
			def grid = new GridEntity(2, 2)
		when:
			def cell1 = grid.cellAt(0, 0)
		then:
			cell1.neighborhood4Way() as Set == [cell1.east, cell1.south] as Set
	}

	def "8-way cell neighborhood linking"() {
		given:
			def grid = new GridEntity(2, 2)
		when:
			def cell1 = grid.cellAt(0, 0)
		then:
			cell1.neighborhood8Way() as Set == [cell1.east, cell1.southeast, cell1.south] as Set
	}
}
