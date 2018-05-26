package io.github.lhanson.possum.entity.message_log

import groovy.transform.InheritConstructors
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.TextEntity

/**
 * A RepeatableMessage is used for text which can be displayed multiple
 * times in a row but where we'd like to condense these repetitions into
 * a single line.
 *
 * Instead of:
 *      "You hit the kobold"
 *      "You hit the kobold"
 * a RepeatableMessage will show:
 *      "You hit the kobold (x2)"
 */
@InheritConstructors
class RepeatableMessage extends TextEntity {
	private int repetitions = 1
	String baseText

	/**
	 * Increment the number of times this message has been sent consecutively
	 */
	void repeat() {
		repetitions++
		setText(baseText + " (x$repetitions)")
	}

	@Override
	boolean addComponentInternal(GameComponent component, boolean publishEvent) {
		if (component instanceof TextComponent) {
			// When the text component is initially added, get its un-incremented state
			baseText = component.text
		}
		super.addComponentInternal(component, publishEvent)
	}

}
