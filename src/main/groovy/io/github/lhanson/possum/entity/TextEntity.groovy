package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.TextComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * An entity which represents some visual text being displayed.
 *
 * Guaranteed to have an {@link AreaComponent} and a {@link TextComponent}.
 */
class TextEntity extends GameEntity {
	Logger log = LoggerFactory.getLogger(this.class)
	// Local references to components we need easy access to
	private AreaComponent areaComponent
	private TextComponent textComponent

	TextEntity() { }

	/**
	 * Shorthand constructor which handles the creation of the appropriate TextComponent
	 *
	 * @param text the text to be set on the auto-generated TextComponent
	 */
	TextEntity(String text) {
		this(text, null)
	}

	/**
	 * Shorthand constructor which handles the creation of the appropriate TextComponent
	 * and setting the relative position of the text.
	 *
	 * @param text the text to be set on the auto-generated TextComponent
	 * @param rpc the relative position of the text (can be null)
	 */
	TextEntity(String text, RelativePositionComponent rpc) {
		components.add(new TextComponent(text))
		if (rpc) {
			components.add(rpc)
		}
	}

	@Override
	GameComponent getComponentOfType(Class requiredType) {
		def result = super.getComponentOfType(requiredType)
		if (!result && requiredType == TextComponent) {
			result = ensureTextComponent()
		} else if (!result && requiredType == AreaComponent) {
			result = ensureAreaComponent()
		}
		return result
	}

	String getText() {
		ensureTextComponent()
		textComponent.text
	}

	void setText(String text) {
		ensureTextComponent()
		textComponent.text = text
		calculateArea()
	}

	private void calculateArea() {
		ensureAreaComponent()
		ensureTextComponent()
		areaComponent.width = textComponent.text.size()
		areaComponent.height = 1
	}

	// We guarantee a TextComponent is present, create one if needed
	private TextComponent ensureTextComponent() {
		TextComponent tc = super.getComponentOfType(TextComponent)
		if (!tc) {
			log.debug "No TextComponent found for text entity $name on initialization, adding one"
			tc = new TextComponent()
			components << tc
		}
		textComponent = tc
		return textComponent
	}

	// We guarantee an AreaComponent is present, create one if needed
	private AreaComponent ensureAreaComponent() {
		AreaComponent ac = super.getComponentOfType(AreaComponent)
		if (!ac) {
			log.debug "No AreaComponent found for text entity $name on initialization, adding one"
			ac = new AreaComponent()
			components << ac
			calculateArea()
		}
		areaComponent = ac
		return areaComponent
	}

	@Override
	String toString() {
		super.toString() + ", text: '$text'"
	}
}
