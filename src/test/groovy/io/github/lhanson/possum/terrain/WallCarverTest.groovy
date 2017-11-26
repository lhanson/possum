package io.github.lhanson.possum.terrain

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GridEntity
import io.github.lhanson.possum.system.MovementSystem
import io.github.lhanson.possum.terrain.maze.BinaryTreeMazeGenerator
import spock.lang.Specification

class WallCarverTest extends Specification {
	def "Build walls produces a field of the expected dimensions"() {
		given:
			int width = 10
			int height = 9
			MovementSystem movementSystem = new MovementSystem()
			GridEntity maze = BinaryTreeMazeGenerator.linkCells(new GridEntity(width, height))
			List<GameEntity> walls = WallCarver.buildWalls(maze)
		when:
			AreaComponent boundingBox = movementSystem.boundingBox(walls.collect { it.getComponentOfType(AreaComponent) })
		then:
			boundingBox.x == 0
			boundingBox.y == 0
			// The number of grid cells is doubled to account for the inserted walls
			boundingBox.width == width * 2
			boundingBox.height == height * 2
	}
}
