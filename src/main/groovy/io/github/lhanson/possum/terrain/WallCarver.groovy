package io.github.lhanson.possum.terrain

import io.github.lhanson.possum.collision.ImpassableComponent
import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GridEntity
import io.github.lhanson.possum.terrain.maze.BinaryTreeMazeGenerator

/**
 * Takes a logical representation of a space; e.g. a grid with linked
 * cells representing open areas and implicit walls represented by
 * the lack of neighboring cells being linked, and * generates the
 * list of entities required to represent walls.
 *
 * Takes GridEntity objects representing a logical space
 * and transforms them into a list of discrete wall entities.
 *
 * Can do literal 1:1 grid-to-wall mapping or it can expand
 * a set of linked cells where walls are implicitly represented
 * by the lack of neighboring cells being linked, as is the
 * case with algorithms like {@link BinaryTreeMazeGenerator}.
 */
class WallCarver {

	/**
	 * @param grid a grid wherein walls are not explicitly represented by cells
	 * @return a set of 2D entities representing the grid walls
	 */
	static List<GameEntity> buildWalls(GridEntity grid) {
		def walls = []
		for (int y = 0; y < grid.height; y++) {
			int tY = 2*y + 1
			for (int x = 0; x < grid.width; x++) {
				GridCellComponent cell = grid.cellAt(x, y)
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
		for (int x = 0; x < 2 * grid.width + 1; x++) {
			walls << buildWall(x, 0)
		}
		// Left wall
		for (int y = 0; y < 2 * grid.height + 1; y++) {
			walls << buildWall(0, y)
		}
		walls
	}

	/**
	 * @param grid a grid wherein walls are explicitly represented by cells
	 * @return a set of 2D entities representing the grid walls
	 */
	static List<GameEntity> getWalls(GridEntity grid, char wallChar = (char) 176/* ░ */) {
		grid.cellList.findResults { GridCellComponent cell ->
			cell.wall ? buildWall(cell.x, cell.y, wallChar) : null
		}
	}

	/**
	 * @param grid a grid wherein floors are explicitly represented by cells
	 * @return a set of 2D entities representing the grid floors
	 */
	static List<GameEntity> getFloors(GridEntity grid, char floorChar = (char) 249 /* ∙ */) {
		grid.cellList.findResults { GridCellComponent cell ->
			!cell.wall ? buildFloor(cell.x, cell.y, floorChar) : null
		}
	}

	static GameEntity buildWall(AreaComponent location, char wallChar = (char) 176/* ░ */) {
		buildWall(location.x, location.y, wallChar)
	}

	static GameEntity buildWall(int x, int y, char wallChar = (char) 176/* ░ */) {
		new GameEntity(
				name: 'wall',
				components: [
						new AreaComponent(x, y, 1, 1),
						new ImpassableComponent(),
						new TextComponent(String.valueOf(wallChar))
				])
	}

	static GameEntity buildFloor(int x, int y, char floorChar = (char) 249/* ∙ */) {
		new GameEntity(
				name: 'floor',
				components: [
						new AreaComponent(x, y, 1, 1),
						new TextComponent(String.valueOf(floorChar))
				])
	}

}
