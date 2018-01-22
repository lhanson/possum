package io.github.lhanson.possum.entity.menu

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.input.MappedInput

/** A menu item representing a button (Ok, Cancel, etc.) */
class ButtonItemEntity extends MenuItemEntity {
	Closure value

	ButtonItemEntity(String label, Closure value) {
		super(label)
		this.label = label
		this.value = value
	}

	@Override
	List<GameEntity> handleInput(MappedInput input) {
		switch (input) {
			case MappedInput.ENTER:
				log.debug "Button $label pressed, executing closure"
				value()
		}
	}

}
