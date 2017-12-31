package io.github.lhanson.possum.collision

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GameEntity
import spock.lang.Specification
import sun.reflect.generics.reflectiveObjects.NotImplementedException


class QuadtreeNodeEntitiesTest extends Specification {
	QuadtreeNodeEntities entities

	def setup() {
		entities = new QuadtreeNodeEntities()
	}

	def "IsEmpty true"() {
		when:
			entities
		then:
			entities.empty
	}

	def "IsEmpty false"() {
		when:
			entities.add(new GameEntity())
		then:
			!entities.empty
	}

	def "Does not contain"() {
		when:
			entities.add(new GameEntity())
		then:
			!entities.contains(new GameEntity())
	}

	def "Iterator"() {
		given:
			entities.addAll([new GameEntity(), new GameEntity(), new GameEntity()])
			int count = 0
		when:
			entities.iterator().each { count++ }
		then:
			count == 3
	}

	def "Get all entities"() {
		given:
			GameEntity entity1 = new GameEntity()
			GameEntity entity2 = new GameEntity()
			GameEntity entity3 = new GameEntity()
			entities.addAll([entity1, entity2, entity3])
		when:
			def results = entities.getAllEntities()
		then:
			results.size() == 3
			results.contains(entity1)
			results.contains(entity2)
			results.contains(entity3)
	}

	def "ToArray"() {
		given:
			GameEntity entity1 = new GameEntity()
			GameEntity entity2 = new GameEntity()
			GameEntity entity3 = new GameEntity()
			entities.addAll([entity1, entity2, entity3])
		when:
			GameEntity[] array = entities.toArray()
		then:
			array.size() == 3
			array[0] == entity1
			array[1] == entity2
			array[2] == entity3
	}

	def "ToArray with param"() {
		when:
			entities.toArray([])
		then:
			thrown NotImplementedException
	}

	def "Add and size"() {
		when:
			entities.add(new GameEntity())
		then:
			entities.size() == 1
	}

	def "Size vs locationCount"() {
		when:
			entities.addAll([
					new GameEntity(components: [new AreaComponent(0, 0, 1, 1)]),
					new GameEntity(components: [new AreaComponent(0, 0, 1, 1)]),
					new GameEntity(components: [new AreaComponent(99, 99, 1, 1)]),
			])
		then:
			entities.size() == 3
			entities.locationCount() == 2
	}

	def "Contains"() {
		given:
			GameEntity entity = new GameEntity()
		when:
			entities.add(entity)
		then:
			entities.contains(entity)
	}

	def "Contains All"() {
		when:
			GameEntity entity1 = new GameEntity()
			GameEntity entity2 = new GameEntity()
			GameEntity entity3 = new GameEntity()
			entities.addAll([entity1, entity2, entity3])
		then:
			entities.containsAll([entity3, entity2, entity1])
	}

	def "Does not ContainsAll"() {
		when:
			GameEntity entity1 = new GameEntity()
			entities.addAll([entity1])
		then:
			!entities.containsAll([entity1, new GameEntity()])
	}

	def "AddAll"() {
		given:
			def entityList = [new GameEntity(), new GameEntity(), new GameEntity()]
		when:
			boolean changed = entities.addAll(entityList)
		then:
			changed
			entities.size() == 3
	}

	def "Location keys aren't subject to external mutation"() {
		given:
			AreaComponent heroOldPos = new AreaComponent(0, 0, 1,1)
			GameEntity hero = new GameEntity(name: 'hero', components: [new AreaComponent(heroOldPos)])
			entities.add(hero)

		when:
			// Modify the area, thus changing its key we would derive for entitiesByLocation
			hero.getComponentOfType(AreaComponent).x = 10
			boolean removed = entities.remove(hero)

		then:
			removed
			entities.size() == 0
	}

	def "Remove"() {
		given:
			GameEntity entity1 = new GameEntity()
			GameEntity entity2 = new GameEntity()
			GameEntity entity3 = new GameEntity()
			entities.addAll([entity1, entity2, entity3])
		when:
			boolean changed = entities.remove(entity1)
		then:
			changed
			!entities.contains(entity1)
			entities.containsAll([entity2, entity3])
	}

	def "Remove nonexistent"() {
		when:
			boolean changed = entities.remove(new GameEntity())
		then:
			!changed
	}

	def "RemoveAll"() {
		given:
			GameEntity entity1 = new GameEntity()
			GameEntity entity2 = new GameEntity()
			GameEntity entity3 = new GameEntity()
			entities.addAll([entity1, entity2, entity3])
		when:
			entities.removeAll([entity1, entity2, new GameEntity()])
		then:
			!entities.contains(entity1)
			entities.containsAll([entity2, entity3])
	}

	def "RetainAll"() {
		given:
			GameEntity entity1 = new GameEntity()
			GameEntity entity2 = new GameEntity()
			GameEntity entity3 = new GameEntity()
			entities.addAll([entity1, entity2, entity3, new GameEntity(), new GameEntity()])
		when:
			entities.retainAll([entity1, entity2, new GameEntity()])
		then:
			entities.size() == 2
			entities.containsAll([entity1, entity2])
			!entities.contains(entity3)
	}

	def "Clear"() {
		given:
			entities.add(new GameEntity())
		when:
			entities.clear()
		then:
			entities.empty
	}

}
