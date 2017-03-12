package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.TextComponent

/**
 * An entity which represents some visual text being displayed
 */
class TextEntity extends GameEntity {

	String getText() {
		TextComponent tc = getComponentOfType(TextComponent)
		tc.text
	}

	void setText(String text) {
		TextComponent tc = getComponentOfType(TextComponent)
		tc.text = text
	}

	AreaComponent calculateArea() {
		TextComponent tc = getComponentOfType(TextComponent)
		AreaComponent ac = super.getComponentOfType(AreaComponent)
		if (ac) {
			ac.width = tc.text.size()
		} else {
			ac = new AreaComponent(0, 0, tc.text.size(), 1)
		}
		return ac
	}

}
