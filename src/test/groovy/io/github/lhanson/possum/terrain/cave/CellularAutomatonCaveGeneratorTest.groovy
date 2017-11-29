package io.github.lhanson.possum.terrain.cave

import spock.lang.Specification

class CellularAutomatonCaveGeneratorTest extends Specification {
	CellularAutomatonCaveGenerator generator

	def setup() {
		generator = new CellularAutomatonCaveGenerator(
				width: 4,
				height: 4,
				initialFactor: 45,
				rand: new Random()
		)
		generator.init(0)
	}

	def "livingNeighbors calculation doesn't include the cell itself"() {
		when:
			generator.grid[0][0] = 1
		then:
			generator.livingNeighbors(0, 0) == 0
	}

	def "livingNeighbors with north living"() {
		when:
			generator.grid[0][0] = 1
		then:
			generator.livingNeighbors(0, 1) == 1
	}

	def "livingNeighbors with east living"() {
		when:
			generator.grid[1][0] = 1
		then:
			generator.livingNeighbors(0, 0) == 1
	}

	def "livingNeighbors with south living"() {
		when:
			generator.grid[0][1] = 1
		then:
			generator.livingNeighbors(0, 0) == 1
	}

	def "livingNeighbors with west living"() {
		when:
			generator.grid[0][0] = 1
		then:
			generator.livingNeighbors(1, 0) == 1
	}

}
