package io.github.lhanson.possum.entity.menu

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.TextEntity

/** Superclass for all menu items */
class MenuItemEntity extends TextEntity {
	String label
	/** Whether this menu entity is currently selected */
	boolean selected

	MenuItemEntity() { }

	MenuItemEntity(String text) {
		super(text)
		this.label = text
	}

	/** Return the current value of this menu item */
	Object getValue() { }

	@Override
	GameComponent getComponentOfType(Class requiredType) {
		def result = super.getComponentOfType(requiredType)
		if (requiredType == TextComponent) {
			// Insert spacing between the label and the value to right-justify the
			// value according to the panel size.
			AreaComponent ac = getComponentOfType(AreaComponent)
			AreaComponent pac = parent.getComponentOfType(AreaComponent)
			int spacing = (pac.width - (parent.padding * 2)) - ac.width + 1
			result.text = label + (' ' * Math.max(1, spacing)) + value
		}
		return result
	}

}
