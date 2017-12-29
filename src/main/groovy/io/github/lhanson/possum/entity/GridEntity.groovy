package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.GridCellComponent

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
				cell.northeast = cellAt(cell.x + 1 , cell.y - 1)
				cell.east  = cellAt(cell.x + 1, cell.y)
				cell.southeast  = cellAt(cell.x + 1, cell.y + 1)
				cell.south = cellAt(cell.x, cell.y + 1)
				cell.southwest = cellAt(cell.x - 1, cell.y + 1)
				cell.west  = cellAt(cell.x - 1, cell.y)
				cell.northwest  = cellAt(cell.x - 1, cell.y - 1)
			}
		}
	}

	GridEntity(int width, int height, int xPos, int yPos) {
		this(width, height)
		components << new AreaComponent(xPos, yPos, width, height)
	}

	/**
	 * Initialize a grid from an existing collection of cells
	 * @param copyCells the cells which will constitute open space in the grid
	 */
	GridEntity(int width, int height, List<GridCellComponent> copyCells) {
		this(width, height)
		cellList.each { it.wall = true }
		copyCells.each { GridCellComponent copyCell ->
			GridCellComponent cell = cellAt(copyCell.x, copyCell.y)
			cell.wall = copyCell.wall
		}
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
