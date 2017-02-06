package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.PositionComponent
import spock.lang.Specification

class MovementSystemTest extends Specification {
	MovementSystem movementSystem = new MovementSystem()

	def "Simple bounding box is correctly computed"() {
		given:
			def positions = [
					new PositionComponent(0, 0),
					new PositionComponent(1, 1)
			]
		when:
			def boundingBox = movementSystem.boundingBox(positions)
		then:
			boundingBox.size() == 2
			boundingBox[0] == new PositionComponent(0, 0)
			boundingBox[1] == new PositionComponent(1, 1)
			boundingBox[1] != new PositionComponent(10, 10)
	}

	def "More complex bounding box is correctly computed"() {
		given:
			def positions = [
					new PositionComponent(100, 10),
					new PositionComponent(101, 15),
					new PositionComponent(200, 100),
					new PositionComponent(130, 500),
					new PositionComponent(100, 10),
			]
		when:
			def boundingBox = movementSystem.boundingBox(positions)
		then:
			boundingBox.size() == 2
			boundingBox[0] == new PositionComponent(100, 10)
			boundingBox[1] == new PositionComponent(200, 500)
	}

}
