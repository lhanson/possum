package io.github.lhanson.possum.component

/**
 * Represents a component able to be positioned with either
 * absolute position or relative alignment.
 */
class PositionComponent extends VectorComponent {
	String name
	Alignment alignment

	PositionComponent(Alignment alignment) {
		this.alignment = alignment
	}
	PositionComponent(int x, int y) {
		super(x, y)
	}
}
