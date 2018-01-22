package io.github.lhanson.possum.entity.menu

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.input.MappedInput

/** A menu item representing an integer value */
class IntegerItemEntity extends MenuItemEntity {
	Integer value

	IntegerItemEntity(String label, int value) {
		super("$label $value")
		this.label = label
		this.value = value
	}

	@Override
	List<GameEntity> handleInput(MappedInput input) {
		switch (input) {
			case MappedInput.LEFT:
				setValue(value - 1)
				log.debug "Decremented $label to $value"
				return [this]
			case MappedInput.RIGHT:
				setValue(value + 1)
				log.debug "Incremented $label to $value"
				return [this]
		}
	}

	void setValue(Integer value) {
		int lengthDelta = String.valueOf(value).length() - String.valueOf(this.value).length()
		this.value = value
		// Update our area if necessary
		if (lengthDelta) {
			AreaComponent ac = getComponentOfType(AreaComponent)
			log.debug "Value string changed length by $lengthDelta characters, adjusting width component"
			ac.width += lengthDelta
		}
	}

}
