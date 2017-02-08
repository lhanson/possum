package io.github.lhanson.possum.component

/**
 * Represents a component able to be positioned by relative coordinates.
 */
class RelativePositionComponent extends VectorComponent {
	/**
	 * @param x the percentage of space from the x origin this entity's center should be placed
	 * @param y the percentage of space from the y origin this entity's center should be placed
	 */
	RelativePositionComponent(int x, int y) {
		super(x, y)
	}


}
