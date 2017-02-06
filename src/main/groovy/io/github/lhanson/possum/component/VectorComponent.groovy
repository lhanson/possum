package io.github.lhanson.possum.component

import mikera.vectorz.Vector2
/**
 * Component representing a 2-dimensional vector
 */
class VectorComponent implements GameComponent {
	Vector2 vector2 = new Vector2()

	VectorComponent() {}

	VectorComponent(int x, int y) {
		vector2.x = x
		vector2.y = y
	}

	Integer getX() {
		vector2.x
	}

	void setX(int x) {
		vector2.x = x
	}

	Integer getY() {
		vector2?.y
	}

	void setY(int y) {
		vector2.y = y
	}

	@Override
	boolean equals(Object v) {
		v instanceof VectorComponent &&
				vector2 == v.vector2
	}

	@Override
	int hashCode() {
		(vector2 != null ? vector2.hashCode() : 0)
	}
}
