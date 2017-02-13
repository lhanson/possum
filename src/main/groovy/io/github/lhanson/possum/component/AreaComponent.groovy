package io.github.lhanson.possum.component

/**
 * An area represented by x, y coordinates and its dimensions.
 */
class AreaComponent extends PositionComponent {
	VectorComponent size

	AreaComponent() {
		super()
		size = new VectorComponent()
	}

	AreaComponent(int x, int y, int width, int height) {
		super(x, y)
		size = new VectorComponent(width, height)
	}

	AreaComponent(PositionComponent pos, int width, int height) {
		super(pos.x, pos.y)
		size = new VectorComponent(width, height)
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
		if (that.x > this.x + this.size.x || // right of this
			that.y > this.y + this.size.y || // below this
			that.x + that.size.x < this.x || // left of this
			that.y + that.size.y < this.y) { // above this
			return false
		}
		return true
	}

	@Override
	String toString() {
		"location ${super.toString()}, size [${size.x}, ${size.y}]"
	}
}
