package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.GridCellComponent
import io.github.lhanson.possum.component.PositionComponent

/**
 * Entity representing a 2-dimensional grid of cells, optionally
 * linked to one or more of their neighbors.
 */
class GridEntity extends GameEntity {
	String name = 'Grid'
	int width, height
	GridCellComponent [][] cells
	List<GridCellComponent> cellList = []
	List<GameComponent> components = []

	GridEntity(int width, int height) {
		this.width = width
		this.height = height
		cells = new GridCellComponent[width][height]
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				cells[x][y] = new GridCellComponent(x, y)
				cellList << cells[x][y]
			}
		}

		// Set neighbor links
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				GridCellComponent cell = cellAt(x, y)
				cell.north = cellAt(cell.x, cell.y - 1)
				cell.south = cellAt(cell.x, cell.y + 1)
				cell.east  = cellAt(cell.x + 1, cell.y)
				cell.west  = cellAt(cell.x - 1, cell.y)
			}
		}
	}

	GridEntity(int width, int height, int xPos, int yPos) {
		this(width, height)
		components << new PositionComponent(xPos, yPos)
	}

	/**
	 * Returns the grid cell specified by the coordinates
	 * @param x the horizontal coordinate
	 * @param y the vertical coordinate
	 * @return the cell at the provided coordinates, or null if none exists
	 */
	GridCellComponent cellAt(int x, int y) {
		if (x >= 0 && x < width &&
		    y >= 0 && y < height) {
			return cells[x][y]
		}
		return null
	}
	GridCellComponent cellAt(double x, double y) {
		cellAt((int)x, (int)y)
	}

	@Override
	List<GameComponent> getComponents() {
		components
	}

	@Override
	String toString() {
		String str = ''
		for (int y = 0; y < height; y++) {
			String row = ''
			for (int x = 0; x < width; x++) {
				row += cellAt(x, y).toString() + "\t"
			}
			str += row.trim() + "\n"
		}
		str
	}
}