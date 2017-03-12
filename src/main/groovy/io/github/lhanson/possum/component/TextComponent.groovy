package io.github.lhanson.possum.component

/**
 * Component used to represent displayed text
 */
class TextComponent implements GameComponent {
	String name = 'textComponent'
	String text = ''

	TextComponent() { }

	TextComponent(String text) {
		this.text = text
	}
}
