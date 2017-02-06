package io.github.lhanson.possum.maze

import io.github.lhanson.possum.component.PositionComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GridEntity
import io.github.lhanson.possum.system.MovementSystem
import spock.lang.Specification

class MazeCarverTest extends Specification {
	def "Build walls produces a field of the expected dimensions"() {
		given:
			int width = 10
			int height = 9
			MovementSystem movementSystem = new MovementSystem()
			GridEntity maze = BinaryTree.linkCells(new GridEntity(width, height))
			List<GameEntity> walls = MazeCarver.buildWalls(maze)
		when:
			def boundingBox = movementSystem.boundingBox(walls.collect { it.getComponentOfType(PositionComponent) })
		then:
			boundingBox[0].x == 0
			boundingBox[0].y == 0
			// The number of grid cells is doubled to account for the inserted walls
			boundingBox[1].x == width * 2
			boundingBox[1].y == height * 2
	}
}
