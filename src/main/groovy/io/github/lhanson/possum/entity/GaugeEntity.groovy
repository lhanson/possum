package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.TextComponent

/**
 * An entity capable of calculating and displaying a value
 */
class GaugeEntity extends TextEntity {
	/** The closure to invoke in order to update the gauge's value */
	Closure update

	GaugeEntity() {
		components << new TextComponent()
	}

}
