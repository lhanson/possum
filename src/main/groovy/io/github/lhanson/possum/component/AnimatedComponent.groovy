package io.github.lhanson.possum.component

class AnimatedComponent implements GameComponent {
	int pulseDurationMillis = 1500
	int currentDuration
	boolean repeat = false

	@Override
	String toString() {
		"AnimatedComponent - pulse duration $pulseDurationMillis, current duration $currentDuration, repeating $repeat"
	}
}
