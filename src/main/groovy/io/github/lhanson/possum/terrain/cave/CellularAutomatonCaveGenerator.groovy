package io.github.lhanson.possum.terrain.cave

import io.github.lhanson.possum.component.GridCellComponent
import io.github.lhanson.possum.entity.GridEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Constructs a cave using a cellular automaton.
 *
 * See: http://www.roguebasin.com/index.php?title=Cellular_Automata_Method_for_Generating_Random_Cave-Like_Levels
 */
@Component
class CellularAutomatonCaveGenerator {
	@Autowired Random rand
	Logger log = LoggerFactory.getLogger(this.class)
	boolean initialized = false
	int[][] grid
	/** The width of the grid to generate */
	int width = 140
	/** The height of the grid to generate */
	int height = 60
	/** The percentage of cells which will be initially alive */
	int initialFactor = 45

	/**
	 * Run the automaton the specified number of generations and create
	 * a GridEntity out of the result.
	 *
	 * @param generations how many generations to run the automaton through
	 * @return a GridEntity representing the final results
	 */
	GridEntity generate(int generations = 10) {
		if (!initialized) {
			init()
		}

		generations.times {
			log.trace "Calculating generation"
			int[][] nextGrid = copyGrid(grid)
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					int livingNeighbors = livingNeighbors(x, y)
					if (isBorder(x, y) ||
							(grid[x][y] == 0 && livingNeighbors >= 5) ||
							(grid[x][y] == 1 && livingNeighbors >= 4)) {
						nextGrid[x][y] = 1
					} else {
						nextGrid[x][y] = 0
					}
				}
			}
			grid = nextGrid
		}

		return getGridEntity()
	}

	/**
	 * Initialize the CA with current parameters.
	 */
	void init(int initialValue = 1) {
		grid = new int[width][height]

		// Set random initial state based on the initialFactor
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (isBorder(x, y) || rand.nextInt(100) <= initialFactor) {
					grid[x][y] = initialValue
				}
			}
		}

		initialized = true
	}

	int[][] copyGrid(int[][] grid) {
		int[][] copy = new int[width][height]
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				copy[x][y] = grid[x][y]
			}
		}
		return copy
	}

	/**
	 * Reports the number of neighboring cells that are alive [excluding the target]
	 */
	int livingNeighbors(int x, int y) {
		int livingNeighbors = 0
		for (int neighborX = x - 1; neighborX <= x + 1; neighborX++) {
			for (int neighborY = y - 1; neighborY <= y + 1; neighborY++) {
				// If inside grid
				if (neighborX >= 0 && neighborX < width && neighborY >= 0 && neighborY < height) {
					// Don't count the cell itself
					if (!(neighborX == x && neighborY == y)) {
						livingNeighbors += grid[neighborX][neighborY]
					}
				}
			}
		}
		return livingNeighbors
	}

	boolean isBorder(int x, int y) {
		x == 0 || x == width - 1 || y == 0 || y == height - 1
	}

	/**
	 * @return a {@link GridEntity} based on the generated grid
	 */
	GridEntity getGridEntity() {
		GridEntity result = new GridEntity(width, height)
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				result.cellAt(x, y).wall = grid[x][y]
			}
		}
		return result
	}

	void analyzeGrid() {
		log.debug "Grid size: $width x $height = ${width * height} total cells"

		int emptyCells = 0
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (grid[x][y] == 0) {
					emptyCells++
				}
			}
		}
		log.debug "Grid is ${(emptyCells / (width * height)) * 100}% open with $emptyCells empty cells"
	}

	def countRooms() {
		def rooms = []
		def openCells = gridEntity.cellList.findAll { !it.wall }
		while (openCells) {
			def cell = openCells.pop()
			def roomCells = GridCellComponent.floodFill(cell)
			rooms.add roomCells
			openCells.removeAll(roomCells)
		}
		return rooms
	}

}
