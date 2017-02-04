package io.github.lhanson.possum.maze

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
class BinaryTree {
	/**
	 * Constructs a maze within the provided grid
	 * @param grid the collection of pre-existing, unlinked cells to operate on
	 * @return the modified grid with linked cells forming a maze
	 */
	static GridEntity linkCells(GridEntity grid) {
		grid.cellList.each { GridCellComponent cell ->
			def neighbors = []
			if (cell.north) neighbors << cell.north
			if (cell.east)  neighbors << cell.east
			// Pick randomly between the north and east neighbor to link
			if (neighbors) {
				cell.link(neighbors[new Random().nextInt(neighbors.size())])
			}
		}
		return grid
	}
}
