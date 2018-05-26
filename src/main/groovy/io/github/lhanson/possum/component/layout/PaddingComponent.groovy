package io.github.lhanson.possum.component.layout

import groovy.transform.ToString
import io.github.lhanson.possum.component.GameComponent

/**
 * Represents an area of padding between the edges of a container
 * and its contents (much like
 * <a href="https://www.w3schools.com/Css/css_padding.asp">CSS padding</a>).
 */
@ToString
class PaddingComponent implements GameComponent {
	int top = 0
	int right = 0
	int bottom = 0
	int left = 0

	PaddingComponent(int padding) {
		top = right = bottom = left = padding
	}

	PaddingComponent(int top, int right, int bottom, int left) {
		this.top = top
		this.right = right
		this.bottom = bottom
		this.left = left
	}

	int getWidth() {
		left + right
	}

	int getHeight() {
		top + bottom
	}
}
