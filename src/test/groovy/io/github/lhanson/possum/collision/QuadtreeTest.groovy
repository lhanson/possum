package io.github.lhanson.possum.collision

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GameEntity
import spock.lang.Specification

class QuadtreeTest extends Specification {
	Quadtree quadtree

	def setup() {
		quadtree = new Quadtree(new AreaComponent(0, 0, 10, 10))
	}

	def "clear"() {
		given:
			GameEntity entity = new GameEntity(components: [new AreaComponent(0, 0, 1,1)])
			quadtree.insert(entity)

		when:
			quadtree.clear()

		then:
			quadtree.entities.isEmpty()
			quadtree.nodes.each { it == null }
	}

	def "Parent has no subtrees before split"() {
		when:
			def tree = new Quadtree()

		then:
			tree.nodes.every { it == null }
	}

	def "Parent contains four subtrees after split"() {
		when:
			quadtree.split()

		then:
			quadtree.nodes.size() == 4
			quadtree.nodes.every { it }
	}

	def "Splits propagate parent node properties to children"() {
		when:
			quadtree.maxObjects = 6
			quadtree.maxLevels = 7
			quadtree.split()

		then:
			quadtree.nodes.every { it.maxObjects == 6 }
			quadtree.nodes.every { it.maxLevels == 7 }
	}

	def "Splits accurately subdivide the existing area"() {
		given:
			quadtree.maxObjects = 1
			quadtree.insert(new GameEntity(components: [new AreaComponent(0, 0, 1,1)]))

		when:
			// Cause a split
			quadtree.insert(new GameEntity(components: [new AreaComponent(9,9, 1,1)]))
			def nodeAreas = quadtree.nodes.collect { return it.bounds.width * it.bounds.height }.sum()

		then:
			quadtree.nodes[0].entities.size() == 1
			quadtree.nodes[3].entities.size() == 1
			nodeAreas == quadtree.bounds.width * quadtree.bounds.height
	}

	def "Splits handle odd dimensions"() {
		given:
			int width = 11
			int height = 11
			quadtree = new Quadtree(new AreaComponent(0, 0, width, height))

		when:
			quadtree.split()
			def nodes = quadtree.nodes

		then:
			nodes[0].bounds.width + nodes[1].bounds.width == width
			nodes[0].bounds.height + nodes[2].bounds.height == height
		and:
			nodes[0].bounds.y + nodes[0].bounds.height == nodes[2].bounds.y
	}

	def "Get index correctly identifies a quadrant given an area"() {
		given:
			// We won't be calculating an index unless we have subnodes, so force a split
			quadtree.split()

		when:
			int multipleQuads = quadtree.getIndex(new AreaComponent(0, 0, 6, 6))
			int upperLeft = quadtree.getIndex(new AreaComponent(0, 0, 1, 1))
			int upperRight = quadtree.getIndex(new AreaComponent(9, 0, 1, 1))
			int lowerLeft = quadtree.getIndex(new AreaComponent(0, 9, 1, 1))
			int lowerRight = quadtree.getIndex(new AreaComponent(9,9, 1, 1))

		then:
			multipleQuads == -1
			upperLeft == 0
			upperRight == 1
			lowerLeft == 2
			lowerRight == 3
	}

	def "Get index when entity is on border of quadrant"() {
		given:
			// We won't be calculating an index unless we have subnodes, so force a split
			quadtree.split()

		when:
			int upperLeft = quadtree.getIndex(new AreaComponent(4, 4, 1, 1))
			int upperRight = quadtree.getIndex(new AreaComponent(5, 4, 1, 1))
			int lowerLeft = quadtree.getIndex(new AreaComponent(4, 5, 1, 1))
			int lowerRight = quadtree.getIndex(new AreaComponent(5, 5, 1, 1))

		then:
			upperLeft == 0
			upperRight == 1
			lowerLeft == 2
			lowerRight == 3
	}

	def "Basic add and retrieve"() {
		given:
			GameEntity entity = new GameEntity(components: [new AreaComponent(0, 0, 1,1)])
			quadtree.insert(entity)

		when:
			def results = quadtree.retrieve(entity.getComponentOfType(AreaComponent))

		then:
			results == [entity]
	}

	def "Add and retrieve with multiple entities"() {
		given:
			quadtree.insert(new GameEntity(components: [new AreaComponent(0, 0, 1,1)]))
			quadtree.insert(new GameEntity(components: [new AreaComponent(1, 0, 1,1)]))

		when:
			def matches = quadtree.retrieve(new AreaComponent(0, 0,5, 5))

		then:
			matches.size() == 2
	}

	def "Insert all"() {
		given:
			quadtree.insertAll([
					new GameEntity(components: [new AreaComponent(0, 0, 1,1)]),
					new GameEntity(components: [new AreaComponent(1, 0, 1,1)])
			])

		when:
			def matches = quadtree.retrieve(new AreaComponent(0, 0,5, 5))

		then:
			matches.size() == 2
	}

	def "Multiple entities in the exact same location are stored as a collection"() {
		given:
			quadtree.maxObjects = 1 // would normally cause a split
			quadtree.insert(new GameEntity(name: 'hero', components: [new AreaComponent(0, 0, 1,1)]))
			quadtree.insert(new GameEntity(name: 'item', components: [new AreaComponent(0, 0, 1,1)]))
			quadtree.insert(new GameEntity(name: 'floor', components: [new AreaComponent(0, 0, 1,1)]))

		when:
			def matches = quadtree.retrieve(new AreaComponent(0, 0,5, 5))

		then:
			matches.size() == 3
			quadtree.countLevels() == 1
	}

	def "Multiple entities in the exact same location are correct after one moves"() {
		given:
			GameEntity hero = new GameEntity(name: 'hero', components: [new AreaComponent(0, 0, 1,1)])
			GameEntity floor = new GameEntity(name: 'floor', components: [new AreaComponent(0, 0, 1,1)])
			quadtree.maxObjects = 1
			quadtree.insert(hero)
			quadtree.insert(floor)

		when:
			quadtree.move(hero, hero.getComponentOfType(AreaComponent), new AreaComponent(1, 0, 1, 1))
			hero.getComponentOfType(AreaComponent).x = 1
			def originalLocation = quadtree.retrieve(new AreaComponent(0, 0,1, 1))
			def newLocation = quadtree.retrieve(new AreaComponent(1, 0,1, 1))

		then:
			originalLocation == [floor]
			newLocation == [hero]
	}

	def "Removal"() {
		given:
			AreaComponent location = new AreaComponent(0, 0, 1, 1)
			GameEntity hero = new GameEntity(name: 'hero', components: [location])
			quadtree.insert(hero)

		when:
			boolean removed = quadtree.remove(hero, location)

		then:
			removed
			quadtree.entities.size() == 0
	}

	def "Entity storage locations aren't vulnerable to external mutation "() {
		given:
			AreaComponent oldPos = new AreaComponent(0, 0, 1,1)
			GameEntity hero = new GameEntity(name: 'hero', components: [new AreaComponent(oldPos)])
			quadtree.insert(hero)

		when:
			AreaComponent newPos = new AreaComponent(1, 0, 1,1)
			hero.removeComponent(oldPos)
			hero.components << newPos
			boolean moved = quadtree.move(hero, oldPos, newPos)
			def oldLocation = quadtree.retrieve(new AreaComponent(0, 0,1, 1))
			def results = quadtree.retrieve(new AreaComponent(1, 0,1, 1))

		then:
			moved
			quadtree.entities.size() == 1
			oldLocation == []
			results == [hero]
	}

	def "Only returns entities in relevant quadrant"() {
		given:
			quadtree.maxObjects = 1 // Force a split
			quadtree.insert(new GameEntity(components: [new AreaComponent(0, 0, 1,1)]))
			quadtree.insert(new GameEntity(components: [new AreaComponent(9,9, 1,1)]))

		when:
			def matches = quadtree.retrieve(new AreaComponent(0, 0,1, 1))

		then:
			matches.size() == 1
	}

	def "Entities spanning subnodes reside in parent node"() {
		given:
			quadtree.maxObjects = 1 // Force a split
			quadtree.insert(new GameEntity(components: [new AreaComponent(0, 0, 1,1)]))
			// Entity spans the split subnodes, and so should be owned by the parent
			quadtree.insert(new GameEntity(components: [new AreaComponent(1,1, 8,8)]))

		when:
			def subnodeMatch = quadtree.retrieve(new AreaComponent(0, 0,1, 1))
			def allMatches = quadtree.retrieve(new AreaComponent(0, 0,10, 10))
			def parentMatch = quadtree.retrieve(new AreaComponent(1, 1,8, 8))

		then:
			subnodeMatch.size() == 1
			allMatches.size() == 2
			parentMatch.size() == 1
	}

	def "Entities moved to subnodes after a split are still searchable"() {
		given:
			quadtree.maxObjects = 1 // Force splits
			quadtree.insert(new GameEntity(components: [new AreaComponent(0, 0, 1,1)]))
			quadtree.insert(new GameEntity(components: [new AreaComponent(9,9, 1,1)]))
			quadtree.insert(new GameEntity(components: [new AreaComponent(9,8, 1,1)]))

		when:
			def matches = quadtree.retrieve(new AreaComponent(9,8, 1,1))

		then:
			matches.size() == 1
	}

	def "Search area covering several quadrants returns all relevant entities"() {
		given:
			quadtree.maxObjects = 1 // Force a split
			quadtree.insert(new GameEntity(components: [new AreaComponent(0, 0, 1,1)]))
			quadtree.insert(new GameEntity(components: [new AreaComponent(9,9, 1,1)]))

		when:
			def matches = quadtree.retrieve(new AreaComponent(0, 0,10, 10))

		then:
			matches.size() == 2
	}

	def "Count levels"() {
		given:
			quadtree.maxObjects = 1 // Force splits
			quadtree.insert(new GameEntity(components: [new AreaComponent(0, 0, 1,1)]))
			// Cause a split
			quadtree.insert(new GameEntity(components: [new AreaComponent(9,9, 1,1)]))
			// Causes three splits because the entities are so close
			quadtree.insert(new GameEntity(components: [new AreaComponent(9,8, 1,1)]))
			// Yet a third split
			quadtree.insert(new GameEntity(components: [new AreaComponent(9, 7, 1,1)]))

		when:
			int levels = quadtree.countLevels()

		then:
			levels == 5
	}

	def "Count entities"() {
		given:
			quadtree.maxObjects = 1 // Force splits
			quadtree.insert(new GameEntity(components: [new AreaComponent(0, 0, 1,1)]))
			quadtree.insert(new GameEntity(components: [new AreaComponent(9, 9, 1,1)]))
			quadtree.insert(new GameEntity(components: [new AreaComponent(1, 0, 1,1)]))
			quadtree.insert(new GameEntity(components: [new AreaComponent(1, 1, 1,1)]))
			quadtree.insert(new GameEntity(components: [new AreaComponent(0, 6, 1,1)]))

		when:
			int entities = quadtree.countEntities()

		then:
			entities == 5
	}

	def "Entities can be removed from the quadtree"() {
		given:
			GameEntity entity = new GameEntity(components: [new AreaComponent(0, 0, 1,1)])
			quadtree.insert(entity)

		when:
			quadtree.remove(entity)
			def results = quadtree.retrieve(entity.getComponentOfType(AreaComponent))

		then:
			results == []
	}

	def "A moving entity is correctly moved to a different node"() {
		given:
			quadtree.maxObjects = 1 // Force splits
			def oldLocation = new AreaComponent(9, 9, 1,1)
			GameEntity mobileEntity = new GameEntity(components: [oldLocation])
			quadtree.insert(mobileEntity)
		and: "Mutate the entity's position"
			def newLocation = new AreaComponent(9, 0, 1,1)
			mobileEntity.removeComponent(oldLocation)
			mobileEntity.components << newLocation

		when:
			def moveSuccess = quadtree.move(mobileEntity, oldLocation, newLocation)
			def entitiesAtOldLocation = quadtree.retrieve(oldLocation)
			def entitiesAtNewLocation = quadtree.retrieve(newLocation)

		then:
			moveSuccess
			entitiesAtOldLocation == []
			entitiesAtNewLocation == [mobileEntity]
	}

	def "Adding an entity outside the present bounds will expand the quadtree"() {
		when:
			AreaComponent location = new AreaComponent(quadtree.bounds.x - 100, 0, 1,1)
			GameEntity entity = new GameEntity(components: [location])

		then:
			quadtree.insert(entity)
			quadtree.bounds.contains location
	}

	def "Expanding the quadtree doesn't break retrieval"() {
		when:
			AreaComponent location = new AreaComponent(quadtree.bounds.x - 100, 0, 1,1)
			GameEntity entity = new GameEntity(components: [location])
			quadtree.insert(entity)

		then:
			quadtree.countEntities() == 1
			quadtree.retrieve(location) == [entity]
	}

	def "Quadtree can return a list of all nodes it contains"() {
		when:
			quadtree.maxObjects = 1 // Force splits
			quadtree.insert(new GameEntity(components: [new AreaComponent(0, 0, 1,1)]))
			// Cause a split
			quadtree.insert(new GameEntity(components: [new AreaComponent(9,9, 1,1)]))
			// Causes three splits because the entities are so close
			quadtree.insert(new GameEntity(components: [new AreaComponent(9,8, 1,1)]))
			// Yet a third split
			quadtree.insert(new GameEntity(components: [new AreaComponent(9, 7, 1,1)]))

		then:
			quadtree.getAllNodeBoundsWithin(quadtree.bounds)
	}

}
