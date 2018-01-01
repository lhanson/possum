package io.github.lhanson.possum.component

/**
 * Component representing a 3-dimensional velocity vector
 */
class VelocityComponent extends VectorComponent implements GameComponent {
	String name = 'VelocityComponent'

	VelocityComponent(int x, int y, int z = 0) {
		super(x, y, z)
	}
}
