package io.github.lhanson.possum.maze

import io.github.lhanson.possum.collision.ImpassableComponent
import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GridEntity

/**
 * Takes a logical representation of a maze; e.g. a grid with linked
 * cells representing passages and implicit walls represented by
 * the lack of neighboring cells being linked, and
 * generates the list of entities required to represent maze walls.
 */
class MazeCarver {
	/**
	 * @param maze a grid wherein walls are not explicitly represented by cells
	 * @return a set of 2D entities representing the maze walls
	 */
	static List<GameEntity> buildWalls(GridEntity maze) {
		def walls = []
		for (int y = 0; y < maze.height; y++) {
			int tY = 2*y + 1
			for (int x = 0; x < maze.width; x++) {
				GridCellComponent cell = maze.cellAt(x, y)
				if (cell) {
					int tX = 2 * x + 1
					if (!cell.isLinked(cell.east)) {
						walls << buildWall(tX + 1, tY)
					}
					if (!cell.isLinked(cell.south)) {
						walls << buildWall(tX, tY + 1)
					}
					// Always build a wall to the SE
					walls << buildWall(tX + 1, tY + 1)
				}
			}
		}
		// Top wall
		for (int x = 0; x < 2 * maze.width + 1; x++) {
			walls << buildWall(x, 0)
		}
		// Left wall
		for (int y = 0; y < 2 * maze.height + 1; y++) {
			walls << buildWall(0, y)
		}
		walls
	}

	static GameEntity buildWall(int x, int y) {
		new GameEntity(
				name: 'wall',
				components: [
						new AreaComponent(x, y, 1, 1),
						new ImpassableComponent(),
						new TextComponent(String.valueOf((char)176)) // ░
				])
	}

}
