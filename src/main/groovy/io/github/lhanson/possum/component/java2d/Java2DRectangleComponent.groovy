package io.github.lhanson.possum.component.java2d

import io.github.lhanson.possum.component.AreaComponent

import java.awt.Graphics

class Java2DRectangleComponent extends Java2DComponent {

	@Override
	void draw(Graphics g, AreaComponent area) {
		g.drawRect(area.x, area.y, area.width, area.height)
	}

}
