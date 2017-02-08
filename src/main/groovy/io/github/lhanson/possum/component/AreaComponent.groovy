package io.github.lhanson.possum.component

/**
 * An area represented by x, y coordinates and its dimensions.
 */
class AreaComponent implements GameComponent {
	VectorComponent location
	VectorComponent size

	AreaComponent(int x, int y, int width, int height) {
		location = new VectorComponent(x, y)
		size = new VectorComponent(width, height)
	}

	/**
	 * Determines whether this area overlaps the provided one
	 * @param area the area to compare against
	 * @return true if the areas overlap, false otherwise
	 */
	boolean overlaps(AreaComponent that) {
		if (that.location.x > this.location.x + this.size.x || // right of this
			that.location.y > this.location.y + this.size.y || // below this
			that.location.x + that.size.x < this.location.x || // left of this
			that.location.y + that.size.y < this.location.y) { // above this
			return false
		}
		return true
	}

	@Override
	String toString() {
		"Location [${location.x}, ${location.y}], size [${size.x}, ${size.y}]"
	}
}
