package io.github.lhanson.possum.terrain.cave

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
	private Logger log = LoggerFactory.getLogger(this.class)
	private int[][] grid
	/** The width of the grid to generate */
	int width = 140
	/** The height of the grid to generate */
	int height = 60
	/** The percentage of cells which will be initially alive */
	int initialFactor = 60

	/**
	 * Run the automaton the specified number of generations and create
	 * a GridEntity out of the result.
	 *
	 * @param generations how many generations to run the automaton through
	 * @return a GridEntity representing the final results
	 */
	GridEntity generate(int generations = 5) {
		init()

		generations.times {
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

		return getGridEntity()
	}

	/**
	 * Initialize the CA with current parameters.
	 */
	void init() {
		grid = new int[width][height]

		// Set random initial state based on the initialFactor
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (isBorder(x, y) || rand.nextInt(100) <= initialFactor) {
					grid[x][y] = 1
				}
			}
		}
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
