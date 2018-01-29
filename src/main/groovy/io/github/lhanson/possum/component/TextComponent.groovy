package io.github.lhanson.possum.component

import groovy.transform.ToString

/**
 * Component used to represent displayed text
 */
@ToString(includes = 'text')
class TextComponent implements GameComponent {
	enum Modifier {
		BOLD
	}

	String name = 'textComponent'
	String text = ''
	Set<Modifier> modifiers

	TextComponent() { }

	TextComponent(String text) {
		this(text, [] as Set)
	}

	TextComponent(String text, Modifier modifier) {
		this(text, [modifier] as Set)
	}

	TextComponent(String text, Set<Modifier> modifiers) {
		this.text = text
		this.modifiers = modifiers
	}

}
