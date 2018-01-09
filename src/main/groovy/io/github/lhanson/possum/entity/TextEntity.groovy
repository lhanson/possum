package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.TextComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * An entity which represents some visual text being displayed
 */
class TextEntity extends GameEntity {
	Logger log = LoggerFactory.getLogger(this.class)
	// Local references to components we need easy access to
	private AreaComponent areaComponent
	private TextComponent textComponent

	@Override
	void init() {
		if (initialized) {
			log.debug "Already initialized $name, skipping"
			return
		}
		super.init()

		if (getComponentOfType(AreaComponent)) {
			areaComponent = getComponentOfType(AreaComponent)
		} else {
			log.debug "No AreaComponent found for text entity $name on initialization, adding one"
			areaComponent = new AreaComponent()
			components << areaComponent
		}

		if (getComponentOfType(TextComponent)) {
			textComponent = getComponentOfType(TextComponent)
		} else {
			log.debug "No TextComponent found for text entity $name on initialization, adding one"
			textComponent = new TextComponent()
			components << textComponent
		}
		calculateArea()
	}

	String getText() {
		textComponent.text
	}

	void setText(String text) {
		textComponent.text = text
		calculateArea()
	}

	private void calculateArea() {
		areaComponent.width = textComponent.text.size()
		areaComponent.height = 1
	}

}
