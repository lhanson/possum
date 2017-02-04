package io.github.lhanson.possum.component

/**
 * Represents a single cell of indeterminately-sized space
 * within a larger grid, with the potential to be spatially
 * linked to other cells in the four cardinal directions.
 */
class GridCellComponent implements GameComponent {
	GridCellComponent north, south, east, west
	List<GridCellComponent> links = []
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
}
