package io.github.lhanson.possum.component

import java.awt.Color

class ColorComponent implements GameComponent {
	Color color

	ColorComponent() { }

	ColorComponent(Color color) {
		this.color = color
	}
}
