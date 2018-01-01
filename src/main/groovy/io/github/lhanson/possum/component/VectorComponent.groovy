package io.github.lhanson.possum.component

import mikera.vectorz.Vector3

/**
 * Component representing a 2-dimensional vector
 */
class VectorComponent implements GameComponent {
	Vector3 vector3 = new Vector3()

	VectorComponent() {}

	VectorComponent(int x, int y, int z = 0) {
		vector3.x = x
		vector3.y = y
		vector3.z = z
	}

	VectorComponent(VectorComponent v) {
		vector3.x = v.x
		vector3.y = v.y
		vector3.z = v.z
	}

	Integer getX() {
		vector3.x
	}

	void setX(int x) {
		vector3.x = x
	}

	Integer getY() {
		vector3?.y
	}

	void setY(int y) {
		vector3.y = y
	}

	Integer getZ() {
		vector3.z
	}

	void setZ(int z) {
		vector3.z = z
	}

	@Override
	boolean equals(Object v) {
		// We're currently only using the z-dimension as a simple way to
		// stack multiple entities on a single 2D location, so this equals
		// represents a comparison in only two dimensions.
		v instanceof VectorComponent &&
				vector3.x == v.vector3.x &&
				vector3.y == v.vector3.y
	}

	@Override
	int hashCode() {
		(vector3 != null ? vector3.hashCode() : 0)
	}

	@Override
	String toString() {
		"[${(int) vector3.x}, ${(int) vector3.y}, ${(int) vector3.z}]"
	}
}
