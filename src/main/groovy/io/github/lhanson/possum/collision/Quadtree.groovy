package io.github.lhanson.possum.collision

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GameEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Data structure for partitioning entities into spacial quadrants
 * for efficient lookup of nearby objects for collision detection.
 *
 * This implementation is focused on rectangles rather than simply points;
 * as such it is geared towards calculations like "give me all entities
 * within a particular bounding box". Of note is the fact that some implementations
 * would return all entities within a matching quadrant; this will
 * filter off any which don't overlap the search area regardless of
 * the implementation details of what quadrants things are stored in.
 *
 * @see <a href="http://gameprogrammingpatterns.com/spatial-partition.html">http://gameprogrammingpatterns.com/spatial-partition.html</a>
 */
class Quadtree {
	static final int DEFAULT_MAX_OBJECTS = 10
	static final int DEFAULT_MAX_LEVELS = 10

	Logger log = LoggerFactory.getLogger(this.class)
	/** Objects a node can hold before it splits */
	int maxObjects = DEFAULT_MAX_OBJECTS
	/** Deepest subnode level allowed */
	int maxLevels = DEFAULT_MAX_LEVELS

	int level = 0
	List<GameEntity> entities = []
	AreaComponent bounds = new AreaComponent()
	Quadtree[] nodes = new Quadtree[4]

	Quadtree() { }

	Quadtree(AreaComponent bounds) {
		this(0, bounds)
	}

	Quadtree(int level, AreaComponent bounds, int maxObjects = DEFAULT_MAX_OBJECTS, int maxLevels = DEFAULT_MAX_LEVELS) {
		entities = []
		this.level = level
		this.bounds = bounds
		this.maxObjects = maxObjects
		this.maxLevels = maxLevels
	}

	/**
	 * Clears the quadtree
	 */
	void clear() {
		entities.clear()
		(0..3).each {
			nodes[it]?.clear()
			nodes[it] = null
		}
	}

	/**
	 * Splits the node into 4 subnodes
	 */
	void split() {
		int subWidth = bounds.width / 2
		int subHeight = bounds.height / 2
		int x = bounds.x
		int y = bounds.y

		nodes[0] = new Quadtree(level + 1, new AreaComponent(x, y, subWidth, subHeight), maxObjects, maxLevels)
		nodes[1] = new Quadtree(level + 1, new AreaComponent(x + subWidth, y, subWidth, subHeight), maxObjects, maxLevels)
		nodes[2] = new Quadtree(level + 1, new AreaComponent(x, y + subHeight, subWidth, subHeight), maxObjects, maxLevels)
		nodes[3] = new Quadtree(level + 1, new AreaComponent(x + subWidth, y + subHeight, subWidth, subHeight), maxObjects, maxLevels)
	}

	/**
	 * Determine which node the object/area belongs to. -1 means
	 * object cannot completely fit within a child node and is part
	 * of the parent node
	 */
	int getIndex(GameEntity entity) {
		getIndex(entity.getComponentOfType(AreaComponent))
	}

	int getIndex(AreaComponent area) {
		int index = -1
		int verticalMidpoint = bounds.x + (bounds.width / 2)
		int horizontalMidpoint = bounds.y + (bounds.height / 2)

		// Object can completely fit within the top quadrants
		boolean topHalf = (area.y < horizontalMidpoint && area.y + area.height <= horizontalMidpoint)
		// Object can completely fit within the bottom quadrants
		boolean bottomHalf = (area.y >= horizontalMidpoint)
		// Object can completely fit within the left quadrants
		if (area.x < verticalMidpoint && area.x + area.width <= verticalMidpoint) {
			if (topHalf) {
				index = 0
			}
			else if (bottomHalf) {
				index = 2
			}
		}
		// Object can completely fit within the right quadrants
		else if (area.x >= verticalMidpoint) {
			if (topHalf) {
				index = 1
			}
			else if (bottomHalf) {
				index = 3
			}
		}

		return index
	}

	/**
	 * Insert the object into the quadtree. If the node
	 * exceeds the capacity, it will split and add all
	 * objects to their corresponding nodes.
	 */
	def insert(GameEntity entity) {
		AreaComponent area = entity.getComponentOfType(AreaComponent)
		if (!bounds.overlaps(area)) {
			log.debug("($level) Not inserting {}; {} is not contained within {}", entity, area, bounds)
			return false
		}

		// If we have a subtree where the entity fits, add it there
		if (nodes[0]) {
			int index = getIndex(area)
			if (index != -1) {
				log.debug("($level) Entity fits subtree $index, recursing")
				return nodes[index].insert(entity)
			}
		}

		entities.add(entity)
		log.debug("($level) Inserted $entity at level $level, have ${entities.size()} out of $maxObjects entities at this level")

		// If we've reached our max threshold, split into subtrees
		if (entities.size() > maxObjects && level < maxLevels) {
			log.debug("($level) Reached object threshold, splitting to level ${level + 1}")
			def remainingEntities = [] // Entities which don't fit in a sub-quadrant
			split()
			entities.each {
				int index = getIndex(it)
				if (index >= 0) {
					log.debug("($level) Moving {} to subtree $index ({})", it.getComponentOfType(AreaComponent), nodes[index].bounds)
					if (!nodes[index].insert(it)) {
						remainingEntities.add(it)
					}
				} else {
					log.debug("($level) Keeping {} at current level as it overlaps subquadrants", it.getComponentOfType(AreaComponent))
					remainingEntities.add(it)
				}
			}
			entities.clear()
			// This node will retain ownership of entities overlapping multiple quadrants
			entities.addAll(remainingEntities)
		}
		return true
	}

	/**
	 * Return all objects within the given area
	 */
	List<GameEntity> retrieve(AreaComponent area) {
		def results = []

		int index = getIndex(area)
		if (index >= 0 && nodes[0]) {
			// Query area fits into a single quadrant
			results.addAll(nodes[index].retrieve(area))
		} else if (index == -1 && nodes[0]) {
			// Query area doesn't fit into a single quadrant, clip to fit each
			nodes.collect { it.bounds.clip(area) }
				.each { AreaComponent clippedArea ->
					results.addAll(nodes[getIndex(clippedArea)].retrieve(clippedArea))
				}
		}
		log.debug("($level) Retrieving, calculated index $index for {}. " +
				"Returning ${results.size()} subnode results and ${entities.size()} from this level", area)
		// Filter results to entities which coincide with our target area
		return (results + entities).findAll {
			it.getComponentOfType(AreaComponent).overlaps(area)
		}
	}

	/** Returns the deepest level the quadtree has expanded to */
	int countLevels() {
		if (nodes[0] == null) {
			return 1 // Bottomed out
		}
		return nodes.collect { it.countLevels() }.max() + 1
	}

	/** Returns the total number of entities contained by this and subtrees */
	int countEntities () {
		int count = entities.size()
		if (nodes[0]) {
			count += nodes.collect { it.countEntities() }.sum()
		}
		return  count
	}

	@Override
	String toString() {
		String s = ""
		if (level == 0) {
			s = "Quadtree statistics:\n" +
				"====================\n" +
					"Max objects per level: $maxObjects; max levels: $maxLevels\n" +
					"Deepest level: ${countLevels()}\n" +
					"Entity count: ${countEntities()}\n\n"
		}
		s += ("\t" * level) + "Quadtree level $level, bounds $bounds, ${entities.size()} entities"
		if (nodes[0]) {
			nodes.each {
				s += "\n$it"
			}
		}
		return s
	}

}
