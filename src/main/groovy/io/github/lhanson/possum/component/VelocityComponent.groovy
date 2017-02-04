package io.github.lhanson.possum.component

/**
 * Component representing a 2-dimensional velocity vector
 */
class VelocityComponent extends VectorComponent implements GameComponent {
	String name = 'VelocityComponent'

	VelocityComponent(int x, int y) {
		super(x, y)
	}
}
