package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
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

	@Override
	GameComponent getComponentOfType(Class requiredType) {
		if (requiredType == AreaComponent) {
			// Gauges have changing text, so we should recalculate its current area
			return calculateArea()
		} else {
			return super.getComponentOfType(requiredType)
		}
	}

}
