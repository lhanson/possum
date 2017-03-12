package io.github.lhanson.possum.rendering

import asciiPanel.AsciiPanel
import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GridEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.RerenderEntity
import io.github.lhanson.possum.entity.TextEntity
import io.github.lhanson.possum.scene.Scene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

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
	// Panel areas in the scene, sorted by x, y coordinates
	List<AreaComponent> scenePanelAreas

	@PostConstruct
	void init() {
		logger.trace "Initializing"
		if (initialViewportSize) {
			viewport = new AreaComponent(0, 0, initialViewportSize.x, initialViewportSize.y)
		} else {
			viewport = new AreaComponent(0, 0, 80, 24)
		}
		terminal = new AsciiPanel(viewport.width, viewport.height)
		logger.debug "Created terminal with viewport {}", viewport

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
		// We're doing active rendering, so we don't need to be told when to repaint
		setIgnoreRepaint(true)
		add(terminal)
		pack()
		setVisible(true)
	}

	void initScene(Scene scene) {
		logger.debug "Initializing scene {}", scene
		scene.getEntitiesMatching([RelativePositionComponent]).each { GameEntity entity ->
			logger.trace "Initializing entity ${entity.name}"
			// Center viewport on focused entity, important that this happens
			// before resolving relatively positioned entities
			GameEntity focusedEntity = scene.entities.find { it.getComponentOfType(CameraFocusComponent) }
			if (focusedEntity) {
				AreaComponent area = focusedEntity.getComponentOfType(AreaComponent)
				if (area) {
					centerViewport(area.position)
				}
			} else {
				// No focused entity, restore default viewport state
				viewport.x = 0
				viewport.y = 0
			}

			AreaComponent ac = entity.getComponentOfType(AreaComponent)
			RelativePositionComponent rpc = entity.getComponentOfType(RelativePositionComponent)
			RelativeWidthComponent rwc = entity.getComponentOfType(RelativeWidthComponent)

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
				InventoryComponent ic = entity.getComponentOfType(InventoryComponent)
				int textHeight = 0
				ic.inventory.each { GameEntity inv ->
					if (inv instanceof TextEntity) {
						textHeight += inv.calculateArea().height
					}
				}
				ac.height = textHeight + 2 // text entries plus borders
				// Compute position
				ac.x = constrainInt((int) (relativeX(rpc) - (ac.width / 2)), 0, viewportWidth - ac.width)
				ac.y = relativeY(rpc)
				// If the calculated position goes outside the viewport because of the panel's height, adjust it up
				if (ac.y + ac.height >= viewportHeight) {
					ac.y -= ((ac.y + ac.height) - viewportHeight)
				}
			} else {
				List<io.github.lhanson.possum.component.TextComponent> text =
						entity.getComponentsOfType(io.github.lhanson.possum.component.TextComponent)
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

		// Store a list of the panel areas in the scene sorted by x,y coordinates for faster reference
		scenePanelAreas = scene.entities
				.findAll { it instanceof PanelEntity }
				.findResults { it.getComponentOfType(AreaComponent) }
		scenePanelAreas.sort { a, b -> a.x <=> b.y ?: a.y <=> b.y }

		// Repaint entire scene
		scene.entities.each { scene.entityNeedsRendering(it) }
		terminal.clear()
	}

	@Override
	void render(Scene scene) {
		StopWatch stopwatch = new StopWatch('rendering')
		logger.trace "Rendering"

		if (scene != lastScene) {
			lastScene = scene
			initScene(scene)
		}

		stopwatch.start('checkScrollBoundaries')
		checkScrollBoundaries(scene)
		stopwatch.stop()

		stopwatch.start('processing each entity')
		scene.entitiesToBeRendered.each { entity ->
			if (entity instanceof GridEntity) {
				// Maze renderer, won't be the same as tile-based game renderer
				renderMaze(entity)
			} else if (entity instanceof PanelEntity) {
				// Panel renderer
				renderPanel(entity)
			} else if (entity instanceof RerenderEntity) {
				AreaComponent area = translateWorldToScreen(entity.getComponentOfType(AreaComponent), viewport)
				logger.debug "Clearing {}", area
				terminal.clear(' ' as char, area.x, area.y, area.width, area.height)
			} else if (entity.parent) {
				TextComponent tc = entity.getComponentOfType(TextComponent)
				AreaComponent pc = entity.parent.getComponentOfType(AreaComponent)
				AreaComponent ac = translateChildToParent(entity.getComponentOfType(AreaComponent), pc)
				if (entity.parent instanceof PanelEntity) {
					ac.x += entity.parent.padding
					ac.y += entity.parent.padding
				}
				write(tc.text, ac.x, ac.y)
			} else {
				// Generic renderer
				if (isVisible(entity)) {
					TextComponent tc = entity.getComponentOfType(TextComponent)
					AreaComponent ac = translateWorldToScreen(entity.getComponentOfType(AreaComponent), viewport)
					write(tc.text, ac.x, ac.y)
				}
			}
		}
		scene.entitiesToBeRendered.clear()
		stopwatch.stop()

		stopwatch.start('terminal render')
		terminal.paintImmediately(0, 0, terminal.width, terminal.height)
		stopwatch.stop()
		logger.trace "Render complete. {}", stopwatch
	}

	boolean isVisible(GameEntity entity) {
		boolean visible = false
		AreaComponent area = entity.getComponentOfType(AreaComponent)
		if (area) {
			// When detecting overlap with panels, we translate the entity's coordinates to
			// be viewport-based rather than world-based.
			AreaComponent viewportArea = translateWorldToScreen(area, viewport)
			visible = area.overlaps(viewport) && !scenePanelAreas.any { viewportArea.overlaps(it) }
		}
		return visible
	}

	/**
	 * Entities with world coordinates need their coordinates translated relative
	 * to the viewport in order to get screen rendering coordinates.
	 *
	 * @param ac the coordinate's entities
	 * @param viewport the position of the viewport in world coordinates
	 * @return an area describing the entity's screen coordinates for rendering
	 */
	AreaComponent translateWorldToScreen(AreaComponent ac, AreaComponent viewport) {
		// world - viewport
		new AreaComponent(ac.x - viewport.x, ac.y - viewport.y, ac.width, ac.height)
	}

	/**
	 * Entities positioned relative to a parent need their coordinates added
	 * to those of the parent in order to get screen rendering coordinates.
	 *
	 * @param child the entity positioned relative to a parent
	 * @param parent the entity whose coordinates determine the child's absolute position
	 * @return an area describing the child's absolute screen coordinates for rendering
	 */
	AreaComponent translateChildToParent(AreaComponent child, AreaComponent parent) {
		// child + panel
		new AreaComponent(child.x + parent.x, child.y + parent.y, child.width, child.height)
	}

	/**
	 * If the focused component gets to a certain viewport threshold, we
	 * recenter the viewport on it before we render the next frame.
 	 */
	void checkScrollBoundaries(Scene scene) {
		GameEntity focusedEntity = scene.getEntityMatching([CameraFocusComponent])
		if (focusedEntity) {
			AreaComponent ac = focusedEntity.getComponentOfType(AreaComponent)
			if (ac) {
				def scrollToX
				def scrollToY
				def threshX = viewport.width * 0.1
				def threshY = viewport.height * 0.1
				if (ac.x < viewport.x + threshX || ac.x > viewport.x + viewport.width - threshX) {
					scrollToX = ac.x
				}
				if (ac.y < viewport.y + threshY || ac.y > viewport.y + viewport.height - threshY) {
					scrollToY = ac.y
				}
				if (scrollToX || scrollToY) {
					def scrollTo = new VectorComponent()
					scrollTo.x = scrollToX ?: viewport.x + viewport.width / 2
					scrollTo.y = scrollToY ?: viewport.y + viewport.height / 2
					centerViewport(scrollTo)
					logger.debug "Scroll boundaries shifted, need rendering"
					scene.entityNeedsRendering(new RerenderEntity(
							name: 'scrollRenderer',
							components: [viewport]
					))
					// Finds non-panel entities for rendering
					scene.findWithin(viewport).each { scene.entityNeedsRendering(it) }
					// Since we're currently adding a RenderEntity for the entire viewport,
					// we need to re-render the panels. Better would be to not clear the
					// panels in the first place.
					scene.entities
							.findAll { it instanceof PanelEntity }
							.each { scene.entityNeedsRendering(it) }
				}
			}
		}
	}

	/**
	 * Writes the given string to the AsciiPanel terminal, omitting any
	 * characters which don't fit.
	 *
	 * @param s the string to write to the screen
	 * @param x the terminal row to begin writing
	 * @param y the terminal column to begin writing
	 */
	void write(String s, int x, int y) {
		for (int i = 0; i < s.size(); i++) {
			if (x + i >= 0 && x + i < viewport.width &&
					y >= 0 && y < viewport.height) {
				terminal.write(s.charAt(i), x + i, y)
			}
		}
	}

	@Override
	int getViewportWidth() {
		viewport.width
	}

	@Override
	int getViewportHeight() {
		viewport.height
	}

	@Override
	void centerViewport(VectorComponent pos) {
		viewport.x = pos.x - (viewport.width / 2)
		viewport.y = pos.y - (viewport.height / 2)
		logger.debug "Centered viewport at $pos; viewport $viewport"
	}

	/**
	 * Renders a panel. Panels' coordinates are viewport-relative,
	 * so no translation from world coordinates is necessary.
	 * @param entity the
	 */
	void renderPanel(PanelEntity panel) {
		AreaComponent ac = panel.getComponentOfType(AreaComponent)
		String h  = String.valueOf((char)205) // ═
		String v  = String.valueOf((char)186) // ║
		String ul = String.valueOf((char)201) // ╔
		String ur = String.valueOf((char)187) // ╗
		String ll = String.valueOf((char)200) // ╚
		String lr = String.valueOf((char)188) // ╝
		// Top border
		terminal.write(ul + ("$h" * (ac.width - 2)) + ur, ac.x, ac.y)
		// Middle
		panel.getComponentOfType(io.github.lhanson.possum.component.InventoryComponent)
				.inventory
				.findAll { it instanceof TextEntity }
				.eachWithIndex { io.github.lhanson.possum.entity.TextEntity entity, int idx ->
			terminal.write(v, ac.x, ac.y + 1 + idx)                // Left border
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
		return (int) ((rpc.y / 100.0f) * viewportHeight) - 1
	}

	int constrainInt(int value, int min, int max) {
		Math.min(Math.max(value, min), max)
	}

}
