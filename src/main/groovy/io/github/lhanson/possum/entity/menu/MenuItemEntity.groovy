package io.github.lhanson.possum.entity.menu

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.TextEntity
import io.github.lhanson.possum.input.MappedInput

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
		if (requiredType == TextComponent && !(this instanceof ButtonItemEntity)) {
			// Insert spacing between the label and the value to right-justify the
			// value according to the panel size.
			AreaComponent ac = getComponentOfType(AreaComponent)
			AreaComponent pac = parent.getComponentOfType(AreaComponent)
			int spacing = (pac.width - (parent.padding * 2)) - ac.width + 1
			result.text = label + (' ' * Math.max(1, spacing)) + value
			log.debug "Right justifying value '$value' for item {}, introduced $spacing spaces, overall text length is {}", label, result.text.length()
		}
		return result
	}

	/**
	 * @param input the mapped input received
	 * @return the list of entities which have been updated
	 */
	List<GameEntity> handleInput(MappedInput input) {
		log.debug "Menu item $label received $input, no-op"
	}

}
