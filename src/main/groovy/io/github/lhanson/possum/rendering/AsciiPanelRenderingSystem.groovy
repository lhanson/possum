package io.github.lhanson.possum.rendering

import asciiPanel.AsciiPanel
import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GridEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.scene.Scene
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
	Scene lastScene
	List<AreaComponent> scenePanelAreas

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
	void render(Scene scene) {
		logger.trace "Rendering"

		if (scene != lastScene) {
			lastScene = scene
			initScene(scene)
		}

		checkScrollBoundaries(
				scene.entities.find { it.getComponentOfType(CameraFocusComponent) }
		)

		// TODO: How to restore what was behind an object?
		// TODO: We need to know which areas are dirty, and also what to paint there.
		// TODO: Double buffering with clipping?
		// TODO: For now, I'll just repaint the whole panel each time.
		terminal.clear()

		scene.entities.each { entity ->
			if (entity instanceof GridEntity) {
				// Maze renderer, won't be the same as tile-based game renderer
				renderMaze(entity)
			} else if (entity instanceof PanelEntity) {
				// Panel renderer
				renderPanel(entity)
			} else {
				// Generic renderer
				if (isVisible(entity)) {
					TextComponent tc = entity.getComponentOfType(TextComponent)
					PositionComponent pc = entity.getComponentOfType(PositionComponent)
					write(tc.text, pc.x, pc.y)
				}
			}
		}

		// TODO: why is this preferable to calling paintImmediately directly?
		update(getGraphics())
		//terminal.paintImmediately(0, 0, terminal.width, terminal.height)
		logger.trace "Render complete"
	}

	@Override
	void update(Graphics g) {
		terminal.paintImmediately(0, 0, terminal.width, terminal.height)
	}

	void initScene(Scene scene) {
		logger.debug "Initializing scene {}", scene
		scene.getEntitiesMatching([RelativePositionComponent]).each { GameEntity entity ->
			logger.trace "Initializing entity ${entity.name}"
			// Center viewport on focused entity, important that this happens
			// before resolving relatively positioned entities
			GameEntity focusedEntity = scene.entities.find { it.getComponentOfType(CameraFocusComponent) }
			if (focusedEntity) {
				PositionComponent position = focusedEntity.getComponentOfType(PositionComponent)
				if (position) {
					centerViewport(position)
				}
			} else {
				// No focused entity, restore default viewport state
				viewport.x = 0
				viewport.y = 0
			}

			AreaComponent ac = entity.getComponentOfType(AreaComponent)
			RelativePositionComponent rpc = entity.getComponentOfType(RelativePositionComponent)
			RelativeWidthComponent rwc = entity.getComponentOfType(RelativeWidthComponent)
			List<io.github.lhanson.possum.component.TextComponent> text =
					entity.getComponentsOfType(io.github.lhanson.possum.component.TextComponent)

			if (!ac) {
				ac = new AreaComponent()
				entity.components.add(ac)
				logger.debug "Added area component for {}", entity.name
			}

			if (rwc) {
				// Compute relative width
				ac.width = (rwc.width / 100.0f) * viewportWidth
			}

			if (entity instanceof PanelEntity) {
				// Compute relative height
				ac.height = text.size() + 2 // text entries plus borders
				// Compute position
				ac.x = constrainInt((int) (relativeX(rpc) - (ac.width / 2)), 0, viewportWidth - ac.width)
				ac.y = relativeY(rpc) - ac.height
			} else {
				int xOffset = 0
				if (text?.get(0)) {
					// This is assuming we want to center this text around its position
					xOffset = text[0].text.length() / 2
					ac.width = text.collect { it.text.size() }.max() // Longest string
					// Assumes each text component is on its own line
					ac.height = text.size()
				}
				ac.x = (rpc.x / 100.0f) * viewportWidth - xOffset
				ac.y = (rpc.y / 100.0f) * viewportHeight
			}
			logger.debug "Calculated position of {} for {}", ac, entity.name
			entity.updateComponentLookupCache()
		}

		// Store a list of the panel areas in the scene for faster reference
		scenePanelAreas = scene.entities
				.findAll { it instanceof PanelEntity }
				.findResults { it.getComponentsOfType(AreaComponent) }
	}

	boolean isVisible(GameEntity entity) {
		boolean visible = false
		AreaComponent area = entity.getComponentOfType(AreaComponent)
		if (area) {
			// When detecting overlap with panels, we translate the entity's coordinates to
			// be viewport-based rather than world-based.
			AreaComponent viewportArea = translateToViewport(area)
			visible = area.overlaps(viewport) && !scenePanelAreas.any { viewportArea.overlaps(it) }
		} else {
			PositionComponent pos = entity.getComponentOfType(PositionComponent)
			if (pos) {
				visible = new AreaComponent(pos.x, pos.y, 1, 1).overlaps(viewport)
			}
		}
		return visible
	}

	AreaComponent translateToViewport(AreaComponent area) {
		int tx = area.x - viewport.x
		int ty = area.y - viewport.y
		new AreaComponent(x: tx, y: ty, width: area.width, height: area.height)
	}

	/**
	 * If the focused component gets to a certain viewport threshold, we
	 * recenter the viewport on it before we render the next frame.
	 * TODO: we should account for panel offset in our thresholds
	 * TODO: perhaps have a function returning the non-panel viewport?
 	 */
	void checkScrollBoundaries(GameEntity focusedEntity) {
		if (focusedEntity) {
			PositionComponent pc = focusedEntity.getComponentOfType(PositionComponent)
			if (pc) {
				def scrollToX
				def scrollToY
				def threshX = viewport.width * 0.1
				def threshY = viewport.height * 0.1
				if (pc.x < viewport.x + threshX || pc.x > viewport.x + viewport.width - threshX) {
					scrollToX = pc.x
				}
				if (pc.y < viewport.y + threshY || pc.y > viewport.y + viewport.height - threshY) {
					scrollToY = pc.y
				}
				if (scrollToX || scrollToY) {
					def scrollTo = new PositionComponent()
					scrollTo.x = scrollToX ?: viewport.x + viewport.width / 2
					scrollTo.y = scrollToY ?: viewport.y + viewport.height / 2
					centerViewport(scrollTo)
				}
			}
		}
	}

	/**
	 * For the given string, prints those of its characters which fall within the viewport.
	 *
	 * @param s the string to write to the screen
	 * @param x the x position of the entity
	 * @param y the y position of the entity
	 */
	void write(String s, int x, int y) {
		// Translate world coordinates into screen coordinates;
		// the viewport may moved around the world
		int tx = x - viewport.x
		int ty = y - viewport.y
		for (int i = 0; i < s.size(); i++) {
			if (tx + i >= 0 && tx + i < viewportWidth &&
			    ty >= 0     && ty < viewportHeight) {
				terminal.write(s.charAt(i), tx + i, ty)
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

	@Override
	void centerViewport(PositionComponent pos) {
		viewport.x = pos.x - (viewport.width / 2)
		viewport.y = pos.y - (viewport.height / 2)
		logger.debug "Centered viewport at $pos; viewport $viewport"
	}

	void renderPanel(PanelEntity entity) {
		AreaComponent ac = entity.getComponentOfType(AreaComponent)
		String h  = String.valueOf((char)205) // ═
		String v  = String.valueOf((char)186) // ║
		String ul = String.valueOf((char)201) // ╔
		String ur = String.valueOf((char)187) // ╗
		String ll = String.valueOf((char)200) // ╚
		String lr = String.valueOf((char)188) // ╝
		// Top border
		terminal.write(ul + ("$h" * (ac.width - 2)) + ur, ac.x, ac.y)
		// Middle
		entity.getComponentsOfType(io.github.lhanson.possum.component.TextComponent)
				.eachWithIndex { io.github.lhanson.possum.component.TextComponent tc, int idx ->
			terminal.write(v, ac.x, ac.y + 1 + idx)             // Left border
			if (tc?.text) {
				terminal.write(tc.text, ac.x + 2, ac.y + 1 + idx)   // Text
			}
			terminal.write(v, ac.x + ac.width - 1, ac.y + 1 + idx) // Right border
		}
		// Bottom border
		terminal.write(ll + ("$h" * (ac.width - 2)) + lr, ac.x, ac.y + (ac.height - 1))
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

	int relativeX(RelativePositionComponent rpc) {
		return (int) ((rpc.x / 100.0f) * viewportWidth)
	}

	int relativeY(RelativePositionComponent rpc) {
		return (int) ((rpc.y / 100.0f) * viewportHeight)
	}

	int constrainInt(int value, int min, int max) {
		Math.min(Math.max(value, min), max)
	}

}
