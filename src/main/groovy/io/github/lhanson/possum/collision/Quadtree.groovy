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
 * Operations supported:
 *      - insert
 *      - query
 *
 * Unsupported:
 *      - move
 *      - remove
 *      - re-balancing after removals
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
		int subWidthLeft = Math.floor(bounds.width / 2)
		int subWidthRight = Math.ceil(bounds.width / 2)
		int subHeightTop = Math.floor(bounds.height / 2)
		int subHeightBottom = Math.ceil(bounds.height / 2)
		int x = bounds.x
		int y = bounds.y

		nodes[0] = new Quadtree(level + 1, new AreaComponent(x, y, subWidthLeft, subHeightTop), maxObjects, maxLevels)
		nodes[1] = new Quadtree(level + 1, new AreaComponent(x + subWidthLeft, y, subWidthRight, subHeightTop), maxObjects, maxLevels)
		nodes[2] = new Quadtree(level + 1, new AreaComponent(x, y + subHeightTop, subWidthLeft, subHeightBottom), maxObjects, maxLevels)
		nodes[3] = new Quadtree(level + 1, new AreaComponent(x + subWidthLeft, y + subHeightTop, subWidthRight, subHeightBottom), maxObjects, maxLevels)
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
		log.debug("($level) Computing index of area {} within total bounds of {}", area, bounds)

		if (nodes[0] != null) {
			if (nodes[0].bounds.contains(area)) {
				return 0
			} else if (nodes[1].bounds.contains(area)) {
				return 1
			} else if (nodes[2].bounds.contains(area)) {
				return 2
			} else if (nodes[3].bounds.contains(area)) {
				return 3
			}
		}
		// area doesn't fit neatly into a child quadrant
		return -1

	}

	/**
	 * Insert the object into the quadtree. If the node
	 * exceeds the capacity, it will split and add all
	 * objects to their corresponding nodes.
	 *
	 * @param entity the entity to insert
	 * @param location the location to add it to; if not provided, it will be looked up from the entity
	 * @return whether insertion was successful.
	 */
	boolean insert(GameEntity entity, AreaComponent location = null) {
		if (!location) {
			location = entity.getComponentOfType(AreaComponent)
		}
		if (!bounds.contains(location)) {
			log.error("($level) Not inserting {}; {} is not contained within {}", entity, location, bounds)
			return false
		}

		// If we have a subtree where the entity fits, add it there
		if (nodes[0]) {
			int index = getIndex(location)
			if (index != -1) {
				log.debug("($level) Entity {} fits subtree $index ({}), recursing", location, nodes[index].bounds)
				return nodes[index].insert(entity, location)
			}
		}

		entities.add(entity)
		log.debug("($level) Inserted $entity ({}) at level $level, have ${entities.size()} out of $maxObjects entities at this level", location)

		// If we've reached our max threshold, split into subtrees
		if (entities.size() > maxObjects && level < maxLevels) {
			log.debug("($level) Reached object threshold, splitting to level ${level + 1}")
			def remainingEntities = [] // Entities which don't fit in a sub-quadrant
			split()
			entities.each {
				int index = getIndex(it)
				if (index >= 0) {
					log.debug("($level) Moving {} to subtree $index ({})", it.getComponentOfType(AreaComponent), nodes[index].bounds)
					nodes[index].insert(it)
				} else {
					log.debug("($level) Keeping {} at current level as it overlaps subquadrants", it.getComponentOfType(AreaComponent))
					remainingEntities.add(it)
				}
			}
			entities.clear()
			// This node will retain ownership of entities overlapping multiple quadrants
			entities.addAll(remainingEntities)
			log.debug("($level) Split complete")
		}
		return true
	}

	/**
	 * Remove the object from the quadtree.
	 *
	 * @param entity the entity to remove
	 * @param location the location to remove it from; if not provided, it will be looked up from the entity
	 * @return whether removal was successful.
	 */
	boolean remove(GameEntity entity, AreaComponent location = null) {
		if (!location) {
			location = entity.getComponentOfType(AreaComponent)
		}

		if (!bounds.contains(location)) {
			log.error("($level) Not removing {}; {} is not contained within {}", entity, location, bounds)
			return false
		}

		// If we have a subtree where the entity fits, remove it there
		if (nodes[0]) {
			int index = getIndex(location)
			if (index != -1) {
				log.debug("($level) Entity {} fits subtree $index ({}), recursing", location, nodes[index].bounds)
				return nodes[index].remove(entity)
			}
		}

		entities.remove(entity)
		log.debug("($level) Removed $entity ({}) at level $level, have ${entities.size()} out of $maxObjects entities at this level", location)
		return true
	}

	/**
	 * Moves the entity from one location to another in the quadtree
	 *
	 * @param entity the entity to move
	 * @param from the entity's previous location
	 * @param to the entity's current location
	 */
	def move(GameEntity entity, AreaComponent from, AreaComponent to) {
		// TODO: This is a naive implementation, we can definitely do better
		log.debug("($level) Moving {} from {} to {}", entity, from, to)
		remove(entity, from)
		insert(entity, to)
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
