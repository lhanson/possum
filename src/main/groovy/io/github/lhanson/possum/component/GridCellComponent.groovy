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
	 * entities representing the 'room' contiguous with the starting point,
	 * both locations which share the target cell's 'wall' state and those
	 * immediately surrounding.
	 *
	 * @return a list of contiguous entities
	 */
	private List<GridCellComponent> floodFindBreadthFirst() {
		def results = []
		Queue<GridCellComponent> queue = new LinkedBlockingQueue([this])

		def examineCell = { GridCellComponent cell ->
			if (cell.visited) return
			// If the cell matches our target state, add it to the queue for processing
			// Otherwise add it to the results as a boundary cell
			if (cell.wall == this.wall) {
				queue.add(cell)
			} else {
				cell.visited = true
				results << cell
			}
		}

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
			while (current?.west && current.west.wall == this.wall) {
				current = current.west
			}
			// Check for a wall on the left
			if (current?.west && !current.west.visited) {
				current.west.visited = true
				results << current.west
			}
			// Move east until we find a non-match
			while (current && current.wall == this.wall) {
				current.visited = true
				results << current
				[current.northwest, current.north, current.northeast, current.southeast, current.south, current.southwest]
					.each { examineCell(it) }
				current = current.east
			}
			// Examine that last non-matching cell in this row
			examineCell(current)
			// Check for a wall on the right
			if (current?.east && !current.east.visited) {
				current.east.visited = true
				results << current.east
			}
		}
		return results
	}

	@Override
	String toString() {
		"($x, $y)"
	}
}
