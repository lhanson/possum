package io.github.lhanson.possum.collision

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GameEntity
import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * A collection of {@link GameEntity} entries stored in a {@link Quadtree} node.
 *
 * We use an abstraction over a collection and not a simple list of entities
 * because we allow the quadtree to store multiple entities with identical
 * locations without counting against a node's total entity count (and thereby
 * not causing spurious node splits) because it doesn't add any lookup cost to
 * store a collection of entries in the same location, e.g. a stack of items on
 * a 2D tile with the same location
 */
class QuadtreeNodeEntities implements Collection<GameEntity> {
	private Map<String, List<GameEntity>> entitiesByLocation = [:]
	// To remove an entity when it's location may have changed, we can't rely on the
	// original derived key so we map the entity to its original key for fast lookup.
	private Map<GameEntity, String> keysByEntity = [:]

	/**
	 * We use an 'x,y,w,h' String as a map key instead of an AreaComponent
	 * reference to avoid problems with using shared mutable state.
	 * @param the location to generate a key for
	 * @result a string representing the location area given
	 */
	private String mapKey(AreaComponent location) {
		"${location?.x},${location?.y},${location?.width},${location?.height}"
	}

	@Override
	int size() {
		entitiesByLocation.values().collect { it.size() }.sum() ?: 0
	}

	/**
	 * Gives the number of distinct locations stored in this node as opposed to
	 * total entities, for use in determining when to split a node into subtrees.
	 *
	 * @return the number of distinct locations stored in this node (not entities)
	 */
	int locationCount() {
		entitiesByLocation.size()
	}

	List<GameEntity> getAllEntities() {
		entitiesByLocation.values().collect().flatten()
	}

	@Override
	boolean isEmpty() {
		size() == 0
	}

	@Override
	boolean contains(Object o) {
		if (!(o instanceof GameEntity)) {
			return false
		}
		GameEntity entity = (GameEntity) o
		String key = mapKey(entity.getComponentOfType(AreaComponent))
		return entitiesByLocation[key]?.contains(entity)
	}

	@Override
	Iterator<GameEntity> iterator() {
		getAllEntities().iterator()
	}

	@Override
	Object[] toArray() {
		getAllEntities().toArray()
	}

	@Override
	<T> T[] toArray(T[] a) {
		throw new NotImplementedException()
	}

	@Override
	boolean add(GameEntity entity) {
		String key = mapKey(entity.getComponentOfType(AreaComponent))
		if (!entitiesByLocation[key]) {
			entitiesByLocation[key] = [entity]
		} else {
			entitiesByLocation[key] << entity
		}
		keysByEntity[entity] = key
		return true
	}

	@Override
	boolean remove(Object o) {
		if (!(o instanceof GameEntity)) {
			return false
		}
		GameEntity entity = (GameEntity) o
		String key = keysByEntity[entity]
		return entitiesByLocation[key]?.remove(o)
	}

	@Override
	boolean containsAll(Collection<?> c) {
		c.every { contains(it) }
	}

	@Override
	boolean addAll(Collection<? extends GameEntity> c) {
		return c.every { add(it) }
	}

	@Override
	boolean removeAll(Collection<?> c) {
		return c.any { remove(it) }
	}

	@Override
	boolean retainAll(Collection<?> c) {
		boolean changed = false
		entitiesByLocation.values().each { List<GameEntity> entities ->
			changed |= entities.retainAll(c)
		}
		return changed
	}

	@Override
	void clear() {
		entitiesByLocation.clear()
	}

	@Override
	String toString() {
		"Node entities[size used for node splits: ${size()}, expandedSize: ${locationCount()}]"
	}

}
