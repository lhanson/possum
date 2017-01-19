package io.github.lhanson.possum.component

import org.springframework.stereotype.Component

@Component
class PositionComponent implements GameComponent {
	String name = 'Position'
	int x
	int y
}
