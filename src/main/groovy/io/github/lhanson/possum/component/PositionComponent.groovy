package io.github.lhanson.possum.component

/**
 * Represents a component able to be positioned with defined coordinates
 */
class PositionComponent extends VectorComponent {
	String name
	PositionComponent(int x, int y) {
		super(x, y)
	}

	@Override
	String toString() {
		"[$x, $y]"
	}
}
