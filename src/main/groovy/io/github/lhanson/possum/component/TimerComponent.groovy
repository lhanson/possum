package io.github.lhanson.possum.component

class TimerComponent implements GameComponent {
	String name = 'timedComponent'
	Integer ticksRemaining
	Closure alarm
}
