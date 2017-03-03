package io.github.lhanson.possum.component

import groovy.transform.Canonical

/**
 * An area represented by x, y coordinates and its dimensions.
 */
@Canonical
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

	@Override
	String toString() {
		"location [$x, $y], size [$width, $height]"
	}
}
