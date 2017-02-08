package io.github.lhanson.possum.rendering

import asciiPanel.AsciiPanel
import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GridEntity
import io.github.lhanson.possum.entity.PanelEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.swing.*
import java.awt.*
import java.util.List

@Component
class AsciiPanelRenderingSystem extends JFrame implements RenderingSystem {
	Logger logger = LoggerFactory.getLogger(this.class)
	AsciiPanel terminal
	@Autowired(required = false)
	VectorComponent initialViewportSize
	AreaComponent viewport

	@PostConstruct
	void init() {
		logger.trace "Initializing"
		if (initialViewportSize) {
			viewport = new AreaComponent(0, 0, initialViewportSize.x, initialViewportSize.y)
		} else {
			viewport = new AreaComponent(0, 0, 80, 24)
		}
		terminal = new AsciiPanel(viewport.size.x, viewport.size.y)
		logger.debug "Created terminal with viewport {}", viewport

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
		setIgnoreRepaint(true)
		add(terminal)
		pack()
		setVisible(true)
	}

	@Override
	void render(List<GameEntity> entities) {
		logger.trace "Rendering"
		// TODO: How to restore what was behind an object?
		// TODO: We need to know which areas are dirty, and also what to paint there.
		// TODO: Double buffering with clipping?
		// TODO: For now, I'll just repaint the whole panel each time.
		terminal.clear()

		entities.each { entity ->
			if (entity instanceof GridEntity) {
				// Maze renderer, won't be the same as tile-based game renderer
				renderMaze(entity)
			} else if (entity instanceof PanelEntity) {
				// Panel renderer
				renderPanel(entity)
			} else {
				// Resolve any relatively positioned entities
				PositionComponent pc = calculatePosition(entity)
				TextComponent tc = entity.components.find { it instanceof TextComponent }
				if (isVisible(pc, tc)) {
					write(tc.text, (int) pc.x, (int) pc.y)
				}
			}
		}

		terminal.repaint()
//		repaint()
//		update(getGraphics())
//		terminal.updateUI()
	}

	boolean isVisible(PositionComponent pc, TextComponent tc) {
		if (pc && tc) {
			AreaComponent area = new AreaComponent(pc.x, pc.y, tc.text.length(), 1)
			return area.overlaps(viewport)
		}
		return false
	}

	/**
	 * For the given string, prints those of its characters which fall within the viewport.
	 *
	 * @param s the string to write to the screen
	 * @param x the x position of the entity
	 * @param y the y position of the entity
	 */
	void write(String s, int x, int y) {
		for (int i = 0; i < s.size(); i++) {
			if (x + i >= 0 && x + i < viewportWidth &&
			    y >= 0     && y < viewportHeight) {
				terminal.write(s.charAt(i), x + i, y)
			}
		}
	}

	@Override
	int getViewportWidth() {
		viewport.size.x
	}

	@Override
	int getViewportHeight() {
		viewport.size.y
	}

	void renderPanel(PanelEntity entity) {
		RelativePositionComponent rpc = entity.getComponentOfType(RelativePositionComponent)
		int width = calculateWidth(entity)
		int height = calculateHeight(entity)
		int x, y // upper left of the panel
		if (rpc) {
			x = constrainInt((int)(relativeX(rpc) - (width / 2)), 0, viewportWidth - width)
			y = relativeY(rpc) - height
		}
		String h  = String.valueOf((char)205) // ═
		String v  = String.valueOf((char)186) // ║
		String ul = String.valueOf((char)201) // ╔
		String ur = String.valueOf((char)187) // ╗
		String ll = String.valueOf((char)200) // ╚
		String lr = String.valueOf((char)188) // ╝
		// Top border
		terminal.write(ul + ("$h" * (width - 2)) + ur, x, y)
		// Middle
		entity.getComponentsOfType(io.github.lhanson.possum.component.TextComponent)
				.eachWithIndex { TextComponent tc, int idx ->
			terminal.write(v, x, y + 1 + idx)             // Left border
			terminal.write(tc.text, x + 2, y + 1 + idx)   // Text
			terminal.write(v, x + width - 1, y + 1 + idx) // Right border
		}
		// Bottom border
		terminal.write(ll + ("$h" * (width - 2)) + lr, x, y + (height - 1))
	}

	/*
	 * Special-case code for rendering mazes as text, without
	 * expanding them with MazeCarver to be actual 2D structures.
	 */
	private void renderMaze(GridEntity grid) {
		def baseX = 0, baseY = 0
		PositionComponent pc = grid.getComponentOfType(PositionComponent)
		if (pc) {
			baseX = (int) pc.x
			baseY = (int) pc.y
		}
		def yOffset = baseY + 1
		def body = '   '
		terminal.write('+' + ('---+' * grid.width), baseX, baseY)
		for (int row = 0; row < grid.height; row++) {
			def top = '|'
			def bottom = '+'
			for (int col = 0; col < grid.width; col++) {
				GridCellComponent cell = grid.cellAt(col, row)
				def eastBoundary = cell.isLinked(cell.east) ? ' ' : '|'
				def southBoundary = cell.isLinked(cell.south) ? '   ' : '---'
				def corner = '+'
				top += body + eastBoundary
				bottom += southBoundary + corner
			}
			terminal.write(top, baseX, yOffset)
			terminal.write(bottom, baseX, yOffset + 1)
			yOffset += 2
		}
	}

	/**
	 * Resolves any relatively positioned entities with respect to the viewport.
	 *
	 * @param entity the entity to position within the viewport
	 * @return the absolute position of the entity
	 */
	PositionComponent calculatePosition(GameEntity entity) {
		PositionComponent pc = entity.getComponentOfType(PositionComponent)
		RelativePositionComponent rpc = entity.getComponentOfType(RelativePositionComponent)
		TextComponent tc = entity.getComponentOfType(io.github.lhanson.possum.component.TextComponent)
		if (!pc && rpc) {
			int xOffset = 0
			if (tc) {
				// This is assuming we want to center this text, which might not hold up for long...
				xOffset = tc.text.length() / 2
			}
			pc = new PositionComponent(
					(int) ((rpc.x / 100.0f) * terminal.widthInCharacters) - xOffset,
					(int) ((rpc.y / 100.0f) * terminal.heightInCharacters)
			)
		}
		return pc
	}

	int calculateWidth(GameEntity entity) {
		RelativeWidthComponent rwc = entity.getComponentOfType(RelativeWidthComponent)
		if (rwc) {
			return (rwc.width / 100.0f) * viewportWidth
		}
	}

	int calculateHeight(GameEntity entity) {
		if (entity instanceof PanelEntity) {
			def text = entity.getComponentsOfType(io.github.lhanson.possum.component.TextComponent)
			return text.size() + 2 // text entries plus borders
		}
	}

	int relativeX(RelativePositionComponent rpc) {
		return (int) ((rpc.x / 100.0f) * terminal.widthInCharacters)
	}

	int relativeY(RelativePositionComponent rpc) {
		return (int) ((rpc.y / 100.0f) * terminal.heightInCharacters)
	}

	int constrainInt(int value, int min, int max) {
		Math.min(Math.max(value, min), max)
	}

}
