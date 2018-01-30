package io.github.lhanson.possum.component

import groovy.transform.Canonical

/**
 * An area represented by x, y, (z) coordinates and its dimensions.
 *
 * The z-dimension is currently only supported in the position value
 * to allow the renderer determine what to draw when multiple entities
 * are stacked on a tile.
 */
@Canonical(includes = 'position, size')
class AreaComponent implements GameComponent {
	VectorComponent position
	VectorComponent size

	AreaComponent() {
		position = new VectorComponent()
		size = new VectorComponent()
	}

	AreaComponent(AreaComponent copy) {
		position = new VectorComponent(copy.position)
		size = new VectorComponent(copy.size)
	}

	AreaComponent(int x, int y, int width, int height) {
		position = new VectorComponent(x, y)
		size = new VectorComponent(width, height)
	}

	AreaComponent(int x, int y, int z, int width, int height) {
		position = new VectorComponent(x, y, z)
		size = new VectorComponent(width, height)
	}

	int getX() {
		position.x
	}

	void setX(int x) {
		this.position.x = x
	}

	int getY() {
		position.y
	}

	void setY(int y) {
		position.y = y
	}

	int getZ() {
		position.z
	}

	void setZ(int z) {
		position.z = z
	}

	int getWidth() {
		size.x
	}

	void setWidth(int width) {
		size.x = width
	}

	int getHeight() {
		size.y
	}

	void setHeight(int height) {
		size.y = height
	}

	int getRight() {
		position.x + size.x
	}

	void setRight(int right) {
		size.x = right - position.x
	}

	int getBottom() {
		position.y + size.y
	}

	void setBottom(int bottom) {
		size.y = bottom - position.y
	}

	/**
	 * Determines whether this area overlaps the provided one
	 * @param area the area to compare against
	 * @return true if the areas overlap, false otherwise
	 */
	boolean overlaps(AreaComponent that) {
		if (that.x >= this.x + this.width  || // right of this
			that.y >= this.y + this.height || // below this
			that.x + that.width <= this.x  || // left of this
			that.y + that.height <= this.y) { // above this
			return false
		}
		return true
	}

	/**
	 * Computes the area of overlap between this area and the provided one
	 * @param that the area to compare with
	 * @return the area of commonality between this and that
	 */
	AreaComponent union(AreaComponent that) {
		def x = Math.max(this.x, that.x)
		def y = Math.max(this.y, that.y)
		def w = Math.min(this.right, that.right) - x
		def h = Math.min(this.bottom, that.bottom) - y
		return new AreaComponent(x, y, w, h)
	}

	/**
	 * Computes whether this area completely contains the provided one
	 * @param that the area we're checking to see if we contain
	 * @return whether this area completely contains the provided one
	 */
	boolean contains(AreaComponent that) {
		this.union(that) == that
	}

	/**
	 * Computes the areas remaining after subtractedAreas are subtracted from this one by
	 * individually subtracting each area from the subject, and then subtracting the overlaps
	 * resulting from these individual decompositions.
	 *
	 * @param subtractedAreas areas to be excluded from the resulting areas
	 * @return a set of areas remaining within the clip area after subtractedAreas are removed
	 */
	Set<AreaComponent> subtractAll(List<AreaComponent> subtractedAreas) {
		Set<AreaComponent> results = []
		if (!subtractedAreas) {
			results << this
		} else {
			def remainders = subtractedAreas.collect { subtract(it) }.flatten()
			// Individual subtractions may result in overlaps between the remainder areas.
			// If any remainders overlap, remove them both from the results and add their union.
			results = resolveOverlaps(remainders)
		}
		return results
	}

	/**
	 * Given a list of areas with possible overlaps, generates a corresponding
	 * list of unique areas without overlap. O(n^2).
	 *
	 * NOTE: At the moment it's not clear to me whether a single pass
	 * will always be sufficient to guarantee zero overlaps.
	 *
	 * @param areas the list of areas to resolve overlaps within
	 * @return a list of unique areas corresponding to the input areas but with no overlaps
	 */
	List<AreaComponent> resolveOverlaps(List<AreaComponent> areas) {
		Set<AreaComponent> overlappers = []
		Set<AreaComponent> subdivisions = []
		areas.eachWithIndex{ AreaComponent ac, int i ->
			// Test against every area to the right of it in the list
			for (int j = i + 1; j < areas.size(); j++) {
				AreaComponent bc = areas[j]
				if (ac.overlaps(bc)) {
					overlappers.addAll([ac, bc])
					subdivisions.add(ac.union(bc))
				}
			}
		}
		return (areas - overlappers) + subdivisions
	}

	/**
	 * Decomposes the subject area into sub-areas excluding the subject area.
	 *
	 * @param subjectArea a subtracted area to be excluded from the resulting subset of areas
	 * @return a set of areas remaining within the clip area after subtractedArea is removed
	 */
	Set<AreaComponent> subtract(AreaComponent subjectArea) {
		if (!subjectArea || !this.overlaps(subjectArea)) {
			return [this] as Set
		}
		// Clip subject to our area
		AreaComponent sa = clip(subjectArea)

		// Up to 4 remainders if it leaves a hole: left, above, right, below
		Set<AreaComponent> remainders = []

		// Left remainder
		if (sa.x > x) {
			remainders << new AreaComponent(x, sa.y, sa.x - x, sa.height)
		}
		// Right remainder
		if (sa.right < right) {
			remainders << new AreaComponent(sa.right, sa.y, right - sa.right, sa.height)
		}
		// Top remainder
		if (sa.y > y) {
			remainders << new AreaComponent(x, y, width, sa.y - y)
		}
		// Bottom remainder
		if (sa.bottom < bottom) {
			remainders << new AreaComponent(x, sa.bottom, width, bottom - sa.bottom)
		}

		return remainders
	}

	/**
	 * If any part of the subject lies outside our area (the clip area),
	 * returns a new area representing only the area of the subject within
	 * the clip area.
	 *
	 * @param subject the area to clip
	 * @return the area of the subject which coincides with the clip rectangle
	 */
	AreaComponent clip(AreaComponent subject) {
		AreaComponent clipped = new AreaComponent(subject)
		if (subject.x < x) {
			clipped.x = x
			clipped.width = subject.width - (x - subject.x)
		}
		if (subject.y < y) {
			clipped.y = y
			clipped.height = subject.height - (y - subject.y)
		}
		if (subject.right > right) {
			clipped.right = right
		}
		if (subject.bottom > bottom) {
			clipped.bottom = bottom
		}
		return clipped
	}

	@Override
	String toString() {
		"location [$x, $y, $z], size [$width, $height]"
	}

}
