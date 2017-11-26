package io.github.lhanson.possum.terrain.maze

import io.github.lhanson.possum.component.GridCellComponent
import io.github.lhanson.possum.entity.GridEntity

/**
 * Constructs a perfect maze using a Binary Tree algorithm.
 * Uses {@link GridEntity}, but in effect creates a binary tree.
 *
 * Note that this is a logical representation of the maze wherein
 * each cell of the grid is used, and 'walls' are only represented
 * implicitly by neighboring cells being unconnected.
 */
class BinaryTreeMazeGenerator {
	/**
	 * Constructs a maze within the provided grid
	 * @param grid the collection of pre-existing, unlinked cells to operate on
	 * @return the modified grid with linked cells forming a maze
	 */
	static GridEntity linkCells(GridEntity grid) {
		for (int y = grid.height - 1; y >= 0; y--) {
			for (int x = 0; x < grid.width; x++) {
				def neighbors = []
				GridCellComponent cell = grid.cellAt(x, y)
				if (cell.north) neighbors << cell.north
				if (cell.east)  neighbors << cell.east
				if (neighbors) {
					// Pick randomly between the north and east neighbor to link
					cell.link(neighbors[new Random().nextInt(neighbors.size())])
				}
			}
		}
		return grid
	}
}
