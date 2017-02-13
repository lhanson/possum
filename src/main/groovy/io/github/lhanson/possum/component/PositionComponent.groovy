package io.github.lhanson.possum.component

/**
 * Represents a component able to be positioned with defined coordinates
 */
class PositionComponent extends VectorComponent {
	PositionComponent() {
		super(0, 0)
	}

	PositionComponent(int x, int y) {
		super(x, y)
	}
}
