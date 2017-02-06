package io.github.lhanson.possum.rendering

import asciiPanel.AsciiPanel
import io.github.lhanson.possum.component.GridCellComponent
import io.github.lhanson.possum.component.PositionComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GridEntity
import io.github.lhanson.possum.system.RenderingSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.swing.*

@Component
class AsciiPanelRenderingSystem extends JFrame implements RenderingSystem {
	Logger logger = LoggerFactory.getLogger(this.class)
	AsciiPanel terminal = new AsciiPanel()

	@PostConstruct
	void init() {
		logger.trace "Initializing"
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
		setIgnoreRepaint(true)
		add(terminal)
		pack()
		setVisible(true)
	}

	@Override
	void render(List<GameEntity> entities) {
		logger.trace "Rendering"
		/*
		if (currentState != gameState.currentMode) {
			logger.debug "Game state change, clearing the terminal"
			terminal.clear()
			currentState = gameState.currentMode
		}
		*/
		// TODO: How to restore what was behind an object?
		// TODO: We need to know which areas are dirty, and also what to paint there.
		// TODO: Double buffering with clipping?
		terminal.clear()
		// TODO: For now, I'll just repaint the whole panel each time.

		entities.each { entity ->
			TextComponent tc = entity.components.find { it instanceof TextComponent }
			PositionComponent pc = entity.components.find { it instanceof PositionComponent }
			RelativePositionComponent rpc = entity.components.find { it instanceof RelativePositionComponent }
			if (tc && (pc || rpc)) {
				if (pc) {
					terminal.write(tc.text, (int) pc.x, (int) pc.y)
				} else {
					int relX = (rpc.x / 100.0f) * terminal.widthInCharacters - (tc.width() / 2)
					int relY = (rpc.y / 100.0f) * terminal.heightInCharacters - (tc.height() / 2)
					terminal.write(tc.text, relX, relY)
				}
			}

			// Maze renderer, won't be the same as tile-based game renderer
			if (entity instanceof GridEntity) {
				GridEntity grid = (GridEntity) entity
				def x = 0, y = 0
				if (pc) {
					x = (int) pc.x
					y = (int) pc.y
				}
				def yOffset = y + 1
				def body = '   '
				terminal.write('+' + ('---+' * grid.width), x, y)
				for (int row = 0; row < grid.width; row++) {
					def top = '|'
					def bottom = '+'
					for (int col = 0; col < grid.height; col++) {
						GridCellComponent cell = grid.cellAt(row, col)
						def eastBoundary = cell.isLinked(cell.east) ? ' ' : '|'
						def southBoundary = cell.isLinked(cell.south) ? '   ' : '---'
						def corner = '+'
						top += body + eastBoundary
						bottom += southBoundary + corner
					}
					terminal.write(top, x, yOffset)
					terminal.write(bottom, x, yOffset + 1)
					yOffset += 2
				}
			}
		}

		terminal.repaint()
//		repaint()
//		update(getGraphics())
//		terminal.updateUI()
	}

	@Override
	int getViewportWidth() {
		terminal.widthInCharacters
	}

	@Override
	int getViewportHeight() {
		terminal.heightInCharacters
	}
}
