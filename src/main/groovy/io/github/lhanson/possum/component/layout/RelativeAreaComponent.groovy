package io.github.lhanson.possum.component.layout

import io.github.lhanson.possum.component.GameComponent

/**
 * Entities possessing a RelativeAreaComponent will have their area
 * resolved according to the dimensions of their enclosing containers.
 *
 * Either a relative value (in percentage of enclosing container) or
 * an absolute value can be specified for either dimension, but not
 * both.
 */
class RelativeAreaComponent implements GameComponent {
	Integer width
	Integer height
	Integer relativeWidth
	Integer relativeHeight

	void setWidth(Integer width) {
		if (relativeWidth != null) {
			throw new IllegalStateException('Cannot set an absolute width when a relative width is already specified')
		}
		this.width = width
	}

	void setRelativeWidth(Integer relativeWidth) {
		if (width != null) {
			throw new IllegalStateException('Cannot set a relative width when an absolute width is already specified')
		}
		if (relativeWidth < 0 || relativeWidth > 100) {
			throw new IllegalArgumentException("Relative width must be between 0 and 100 (inclusive). Got $relativeWidth")
		}
		this.relativeWidth = relativeWidth
	}

	void setHeight(Integer height) {
		if (relativeHeight != null) {
			throw new IllegalStateException('Cannot set an absolute height when a relative height is already specified')
		}
		this.height = height
	}

	void setRelativeHeight(Integer relativeHeight) {
		if (height != null) {
			throw new IllegalStateException('Cannot set a relative height when an absolute height is already specified')
		}
		if (relativeHeight < 0 || relativeHeight > 100) {
			throw new IllegalArgumentException("Relative height must be between 0 and 100 (inclusive). Got $relativeHeight")
		}
		this.relativeHeight = relativeHeight
	}
}
