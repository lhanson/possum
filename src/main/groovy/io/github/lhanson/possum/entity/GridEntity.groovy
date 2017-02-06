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
		for (int row = 0; row < width; row++) {
			for (int col = 0; col < height; col++) {
				cells[row][col] = new GridCellComponent(row, col)
				cellList << cells[row][col]
			}
		}

		// Set neighbor links
		cellList.each { GridCellComponent cell ->
			cell.north = cellAt(cell.x - 1, cell.y)
			cell.south = cellAt(cell.x + 1, cell.y)
			cell.east  = cellAt(cell.x, cell.y + 1)
			cell.west  = cellAt(cell.x, cell.y - 1)
		}
	}

	GridEntity(int width, int height, int xPos, int yPos) {
		this(width, height)
		components << new PositionComponent(xPos, yPos)
	}

	/**
	 * Returns the grid cell specified by the coordinates
	 * @param row the row coordinate
	 * @param col the column coordinate
	 * @return the cell at the provided coordinates, or null if none exists
	 */
	GridCellComponent cellAt(int row, int col) {
		if (row >= 0 && row < width &&
		    col >= 0 && col < height) {
			return cells[row][col]
		}
		return null
	}
	GridCellComponent cellAt(double row, double col) {
		cellAt((int)row, (int)col)
	}

	@Override
	List<GameComponent> getComponents() {
		components
	}
}
