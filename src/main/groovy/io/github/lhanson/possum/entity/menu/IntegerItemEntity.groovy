package io.github.lhanson.possum.entity.menu

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.input.MappedInput

/** A menu item representing an integer value */
class IntegerItemEntity extends MenuItemEntity {
	Integer value
	Integer min
	Integer max

	IntegerItemEntity(String label, Integer value) {
		this(label, value, null, null)
	}

	IntegerItemEntity(String label, Integer value, Integer min) {
		this(label, value, min, null)
	}

	IntegerItemEntity(String label, Integer value, Integer min, Integer max) {
		super("$label $value")
		this.label = label
		this.value = value
		this.min = min
		this.max = max
		if ((min != null && value < min) || (max != null && value > max)) {
			throw new IllegalArgumentException("Value $value is outside the acceptable range of $min to $max")
		}
	}

	@Override
	List<GameEntity> handleInput(MappedInput input) {
		switch (input) {
			case MappedInput.LEFT:
				if (setValue(value - 1)) {
					log.debug "Decremented $label to $value"
					return [this]
				}
				break
			case MappedInput.RIGHT:
				if (setValue(value + 1)) {
					log.debug "Incremented $label to $value"
					return [this]
				}
		}
	}

	/**
	 * @param value the new value to set
	 * @return whether or not the value was set successfully (can fail if outside min/max)
	 */
	boolean setValue(Integer value) {
		if ((min != null && value < min) || (max != null && value > max)) {
			log.debug "Attempted to set value ($value) outside acceptable range of $min to $max, ignoring"
			return false
		}

		int lengthDelta = String.valueOf(value).length() - String.valueOf(this.value).length()
		this.value = value
		// Update our area if necessary
		if (lengthDelta) {
			AreaComponent ac = getComponentOfType(AreaComponent)
			log.debug "Value string for $label changed length by $lengthDelta characters, adjusting width component"
			ac.width += lengthDelta
		}
		return true
	}

}
