package io.github.lhanson.possum.component

import groovy.transform.TailRecursive

/**
 * Represents a single cell of indeterminately-sized space
 * within a larger grid, with the potential to be spatially
 * linked to other cells in the four cardinal directions.
 */
class GridCellComponent implements GameComponent {
	GridCellComponent north, northeast, east, southeast, south, southwest, west, northwest
	List<GridCellComponent> links = []
	/** Whether this cell represents a wall */
	boolean wall = false
	boolean visited = false
	int x, y

	GridCellComponent(int x, int y) {
		this.x = x
		this.y = y
	}

	void link(GridCellComponent cell, boolean bidirectional = true) {
		links << cell
		if (bidirectional) {
			cell.link(this, false)
		}
	}

	void unlink(GridCellComponent cell, boolean bidirectional) {
		links.remove(cell)
		if (bidirectional) {
			cell.unlink(this, false)
		}
	}

	boolean isLinked(GridCellComponent cell) {
		links.contains(cell)
	}

	/** Returns all of the non-null neighboring cells in 4 directions */
	List<GridCellComponent> neighborhood4Way() {
		[north, south, east, west] - null
	}

	/** Returns all of the non-null neighboring cells in 8 directions */
	List<GridCellComponent> neighborhood8Way() {
		[north, northeast, east, southeast, south, southwest, west, northwest] - null
	}

	/**
	 * Performs a flood fill beginning at this cell.
	 *
	 * @return all cells in the cell's "room", defined by whether it's a wall or not
	 */
	List<GridCellComponent> floodFill() {
		floodFillRecursive(this, neighborhood8Way())
	}

	@TailRecursive
	private List<GridCellComponent> floodFillRecursive(GridCellComponent cell, unvisited = [], results = []) {
		cell.visited = true
		results << cell
		unvisited.remove(cell)
		unvisited.addAll(cell.neighborhood8Way().findAll { !it.visited && it.wall == cell.wall })
		if (unvisited.empty) {
			return results
		} else {
			return floodFillRecursive(unvisited.pop(), unvisited, results)
		}
	}

	@Override
	String toString() {
		"($x, $y)"
	}
}
