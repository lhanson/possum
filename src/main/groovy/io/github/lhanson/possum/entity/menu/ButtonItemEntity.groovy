package io.github.lhanson.possum.entity.menu

import io.github.lhanson.possum.component.layout.RelativePositionComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.input.MappedInput

/** A menu item representing a button (Ok, Cancel, etc.) */
class ButtonItemEntity extends MenuItemEntity {
	Closure value

	ButtonItemEntity(String label, Closure value) {
		this(label, value, null)
	}

	ButtonItemEntity(String label, Closure value, RelativePositionComponent rpc) {
		this(label, value, rpc, false)
	}

	ButtonItemEntity(String label, Closure value, RelativePositionComponent rpc, boolean selected) {
		super(label, rpc)
		this.label = label
		this.value = value
		this.selected = selected
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
