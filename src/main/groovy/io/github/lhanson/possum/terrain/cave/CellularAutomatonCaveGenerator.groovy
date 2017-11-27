package io.github.lhanson.possum.terrain.cave

import io.github.lhanson.possum.entity.GridEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Constructs a cave using a cellular automaton.
 *
 * See: http://www.roguebasin.com/index.php?title=Cellular_Automata_Method_for_Generating_Random_Cave-Like_Levels
 */
class CellularAutomatonCaveGenerator {
	Logger log = LoggerFactory.getLogger(this.class)
	Random rand
	int[][] grid
	int width
	int height

	/**
	 * Runs a cellular automaton system to generate a grid of the specified dimensions.
	 *
	 * @param width the width of the generated grid
	 * @param height the height of the generated grid
	 * @param initialFactor the percentage of cells which will be initially alive
	 * @param generations the number of generations to run
	 */
	CellularAutomatonCaveGenerator(int width, int height, Long randomSeed = null, int initialFactor = 60, int generations = 5) {
		this.width = width
		this.height = height
		grid = new int[width][height]
		rand = new Random(randomSeed ?: System.currentTimeMillis())

		// Set random initial state based on the initialFactor
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (isBorder(x, y) || rand.nextInt(100) <= initialFactor) {
					grid[x][y] = 1
				}
			}
		}

		generations.times { generate() }
	}

	/**
	 * Advance the CA universe a single step
	 */
	void generate() {
		log.trace "Calculating generation"
		int[][] nextGrid = copyGrid(grid)
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (livingNeighbors(x, y) >= 5 || /*livingNeighbors(x, y) == 0 ||*/ isBorder(x, y)) {
					nextGrid[x][y] = 1
				} else {
					nextGrid[x][y] = 0
				}
			}
		}
		grid = nextGrid
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

}
