package io.github.lhanson.possum.textpanel

import io.github.lhanson.possum.entity.GameEntity

class PanelBuilder {
	/**
	 * @param maze a grid wherein walls are not explicitly represented by cells
	 * @return a set of 2D entities representing the maze walls
	 */
	static List<GameEntity> buildPanel(int x, int y, int width, int height) {
		println "Building a panel at $x, $y of dimensions $width, $height"
	}
}
