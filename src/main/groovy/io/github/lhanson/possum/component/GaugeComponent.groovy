package io.github.lhanson.possum.component

// TODO: a component which tracks a value
class GaugeComponent extends TextComponent {
	Closure update

	GaugeComponent() { }

	GaugeComponent(String text) {
		super(text)
	}
}
