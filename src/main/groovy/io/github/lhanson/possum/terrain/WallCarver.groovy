package io.github.lhanson.possum.terrain

import io.github.lhanson.possum.collision.ImpassableComponent
import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GridEntity
import io.github.lhanson.possum.terrain.maze.BinaryTreeMazeGenerator
import org.springframework.stereotype.Component

/**
 * Takes a logical representation of a space; e.g. a grid with linked
 * cells representing open areas and implicit walls represented by
 * the lack of neighboring cells being linked, and generates the
 * list of entities required to represent walls and floors.
 *
 * Can do literal 1:1 grid-to-wall mapping or it can expand
 * a set of linked cells where walls are implicitly represented
 * by the lack of neighboring cells being linked, as is the
 * case with algorithms like {@link BinaryTreeMazeGenerator}.
 */
@Component
class WallCarver {

	/**
	 * Takes a sparse representation of a maze where every cell represents a floor
	 * tile and wall tiles are implied by cells being non-linked, and generates
	 * a full list of entities for all of the floor and wall tiles.
	 *
	 * @param grid a grid wherein walls are not explicitly represented by cells
	 * @return a set of 2D entities representing the grid's floor and wall cells
	 */
	List<GameEntity> buildGridFromMaze(GridEntity grid) {
		def entities = []
		for (int y = 0; y < grid.height; y++) {
			int tY = 2*y + 1
			for (int x = 0; x < grid.width; x++) {
				GridCellComponent cell = grid.cellAt(x, y)
				if (cell) {
					int tX = 2 * x + 1
					entities << buildFloor(tX, tY)
					if (cell.isLinked(cell.east)) {
						entities << buildFloor(tX + 1, tY)
					} else {
						entities << buildWall(tX + 1, tY)
					}
					if (cell.isLinked(cell.south)) {
						entities << buildFloor(tX, tY + 1)
					} else {
						entities << buildWall(tX, tY + 1)
					}
					// Always build a wall to the SE
					entities << buildWall(tX + 1, tY + 1)
				}
			}
		}
		// Top wall
		for (int x = 0; x < 2 * grid.width + 1; x++) {
			entities << buildWall(x, 0)
		}
		// Left wall
		for (int y = 0; y < 2 * grid.height + 1; y++) {
			entities << buildWall(0, y)
		}

		entities
	}

	/**
	 * @param cellList a list of cells having a boolean 'wall' status
	 * @return a set of 2D entities representing the floors and walls
	 */
	List<GameEntity> getTiles(List<GridCellComponent> cellList, char floorChar = (char) 249 /* ∙ */, char wallChar = (char) 176/* ░ */) {
		cellList.findResults { GridCellComponent cell ->
			cell.wall ? buildWall(cell.x, cell.y, wallChar) : buildFloor(cell.x, cell.y, floorChar)
		}
	}

	/**
	 * @param grid a list of cells having a boolean 'wall' status
	 * @return a set of 2D entities representing the grid walls
	 */
	List<GameEntity> getWalls(GridEntity grid, char wallChar = (char) 176/* ░ */) {
		grid.cellList.findResults { GridCellComponent cell ->
			cell.wall ? buildWall(cell.x, cell.y, wallChar) : null
		}
	}

	/**
	 * @param grid a list of cells having a boolean 'wall' status
	 * @return a set of 2D entities representing the grid floors
	 */
	List<GameEntity> getFloors(GridEntity grid, char floorChar = (char) 249 /* ∙ */) {
		grid.cellList.findResults { GridCellComponent cell ->
			!cell.wall ? buildFloor(cell.x, cell.y, floorChar) : null
		}
	}

	GameEntity buildWall(AreaComponent location, char wallChar = (char) 176/* ░ */) {
		buildWall(location.x, location.y, wallChar)
	}

	GameEntity buildWall(int x, int y, char wallChar = (char) 176/* ░ */) {
		new GameEntity(
				name: 'wall',
				components: [
						new AreaComponent(x, y, 1, 1),
						new ImpassableComponent(),
						new TextComponent(String.valueOf(wallChar))
				])
	}

	GameEntity buildFloor(int x, int y, char floorChar = (char) 249/* ∙ */) {
		new GameEntity(
				name: 'floor',
				components: [
						new AreaComponent(x, y, 1, 1),
						new TextComponent(String.valueOf(floorChar))
				])
	}

}
