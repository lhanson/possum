package io.github.lhanson.possum.component

/**
 * Component used to represent displayed text
 */
class TextComponent implements GameComponent {
	String name = 'textComponent'
	String text

	TextComponent(String text) {
		this.text = text
	}

	int width() {
		text?.length()
	}
	int height() {
		1
	}
}
