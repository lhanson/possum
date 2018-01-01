package io.github.lhanson.possum.component

import groovy.transform.TailRecursive

import java.util.concurrent.LinkedBlockingQueue

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
	List<GridCellComponent> floodFind() {
		floodFindBreadthFirst()
	}

	// NOTE: this appears to break deterministic random seed behavior
	@TailRecursive
	private List<GridCellComponent> floodFindRecursive(GridCellComponent cell, unvisited = [], results = []) {
		cell.visited = true
		results << cell
		unvisited.remove(cell)
		unvisited.addAll(cell.neighborhood8Way().findAll { !it.visited && it.wall == cell.wall })
		if (unvisited.empty) {
			return results
		} else {
			return floodFindRecursive(unvisited.pop(), unvisited, results)
		}
	}

	/**
	 * Performs a breadth-first queue-based flood find and returns
	 * entities representing those contiguous with the starting point.
	 *
	 * @return a list of contiguous entities
	 */
	private List<GridCellComponent> floodFindBreadthFirst() {
		def results = []
		Queue<GridCellComponent> queue = new LinkedBlockingQueue([this])
		while (!queue.empty) {
			// Start a new row
			def current = queue.remove()
			/*
			 * For reasons I haven't quite sorted out, we see cells coming
			 * off the queue which have already been visited.
			 * Skip these.
			 */
			while (current?.visited) {
				current = queue.poll()
			}
			if (!current) break

			// Move west until we find a non-match
			while (current.west && current.west.wall == wall) {
				current = current.west
			}
			// Move east until we find a non-match
			while (current && current.wall == wall) {
				current.visited = true
				results << current
				// If north is a match, add it to the queue
				if (current.north.wall == wall && !current.north.visited) {
					queue.add(current.north)
				}
				// If south is a match, add it to the queue
				if (current.south.wall == wall && !current.south.visited) {
					queue.add(current.south)
				}
				current = current.east
			}
		}
		return results
	}

	@Override
	String toString() {
		"($x, $y)"
	}
}
