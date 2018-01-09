package io.github.lhanson.possum.rendering

import asciiPanel.AsciiPanel
import io.github.lhanson.possum.collision.Quadtree
import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GaugeEntity
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
import javax.imageio.ImageIO
import javax.swing.*
import java.awt.*
import java.awt.image.BufferedImage
import java.util.List

@Component
class AsciiPanelRenderingSystem extends JFrame implements RenderingSystem {
	Logger logger = LoggerFactory.getLogger(this.class)
	AsciiPanel terminal
	@Autowired(required = false)
	VectorComponent initialViewportSize
	// the entire visible area being rendered (in world coordinates, not screen pixels)
	AreaComponent viewport
	// Panel areas in the scene, sorted by x, y coordinates
	List<AreaComponent> scenePanelAreas

	@PostConstruct
	void init() {
		logger.trace "Initializing"
		if (initialViewportSize) {
			viewport = new AreaComponent(0, 0, Integer.MIN_VALUE, initialViewportSize.x, initialViewportSize.y)
		} else {
			viewport = new AreaComponent(0, 0, Integer.MIN_VALUE,100, 40)
		}
		terminal = new AsciiPanel(viewport.width, viewport.height)
		logger.debug "Created terminal with viewport {}", viewport

		boolean isOSX = false
		try {
			String className = "com.apple.eawt.Application"
			Class<?> cls = Class.forName(className)
			isOSX = true
			Object application = cls.newInstance().getClass().getMethod("getApplication")
					.invoke(null)
			BufferedImage image = ImageIO.read(ClassLoader.getResourceAsStream('/possum.jpg'))
			application.getClass().getMethod("setDockIconImage", java.awt.Image)
					.invoke(application, image)
		}
		catch (Throwable t) {
			if (isOSX) {
				logger.error("Error setting dock icon image", t)
			}
		}

		setBackground(Color.BLACK)
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
		// We're doing active rendering, so we don't need to be told when to repaint
		setIgnoreRepaint(true)
		setResizable(false)
		add(terminal)
		pack()
	}

	@Override
	void initScene(Scene scene) {
		logger.debug "Initializing scene {}", scene
		long startTime = System.currentTimeMillis()

		resolveRelativePositions(scene)

		// Store a list of the panel areas in the scene sorted by x,y coordinates for faster reference
		scenePanelAreas = scene.panels.findResults { it.getComponentOfType(AreaComponent) }
		scenePanelAreas.sort { a, b -> a.x <=> b.y ?: a.y <=> b.y }

		viewport.x = 0
		viewport.y = 0

		// Repaint entire scene
		repaintScene(scene)
		terminal.clear()
		setVisible(true)
		logger.debug "Renderer initialization took ${System.currentTimeMillis() - startTime} ms"
	}

	@Override
	void uninitScene(Scene scene) {
		scenePanelAreas = null
	}

	/**
	 * When initially rendering a scene (or after we scroll the viewport),
	 * queue all visible entities for rendering.
	 *
	 * @param scene the scene to do a complete rerender on
	 */
	def repaintScene(Scene scene) {
		// Visible non-panel entities
		scene.findNonPanelWithin(viewport)
				.findAll { isVisible(it) }
				.each { scene.entityNeedsRendering(it) }

		// Panels and their contents
		scene.panels.each {
			scene.entityNeedsRendering(it)
			InventoryComponent ic = it.getComponentOfType(InventoryComponent)
			ic.inventory.each { item -> scene.entityNeedsRendering(item) }
		}

		// Hint to the render method that we can use one large dirty rectangle
		scene.entityNeedsRendering(new RerenderEntity(name: 'scrollRenderer'))
	}

	/**
	 * Finds relatively-positioned entities within the scene and assigns them concrete
	 * AreaComponents based on the viewport.
	 *
	 * @param scene the scene we're initializing
	 */
	void resolveRelativePositions(Scene scene) {
		// Center viewport on focused entity, important that this happens
		// before resolving relatively positioned entities
		GameEntity focusedEntity = scene.entities.find { it.getComponentOfType(CameraFocusComponent) }
		if (focusedEntity) {
			AreaComponent area = focusedEntity.getComponentOfType(AreaComponent)
			if (area) {
				centerViewport(area.position)
			}
		}

		scene.getEntitiesMatching([RelativePositionComponent]).each { GameEntity entity ->
			logger.trace "Initializing relatively positioned entity ${entity.name}"

			AreaComponent parentReference
			if (entity.parent) {
				parentReference = new AreaComponent(entity.parent.getComponentOfType(AreaComponent))
			} else {
				parentReference = new AreaComponent(viewport)
			}

			AreaComponent ac = entity.getComponentOfType(AreaComponent)
			RelativePositionComponent rpc = entity.getComponentOfType(RelativePositionComponent)
			RelativeWidthComponent rwc = entity.getComponentOfType(RelativeWidthComponent)

			boolean newArea = false
			if (!ac) {
				ac = new AreaComponent()
				newArea = true
			}

			if (rwc) {
				// Compute relative width
				ac.width = (rwc.width / 100.0f) * parentReference.width
			}

			if (entity instanceof PanelEntity) {
				// Compute relative height
				InventoryComponent ic = entity.getComponentOfType(InventoryComponent)
				int textHeight = 0
				ic?.inventory.each { GameEntity inv ->
					if (inv instanceof TextEntity) {
						AreaComponent area = inv.getComponentOfType(AreaComponent)
						textHeight += area.height
						area.x = entity.padding
					}
				}
				ac.height = textHeight + 2 // text entries plus borders
				// Compute position
				ac.x = constrainInt((int) (relativeX(rpc) - (ac.width / 2)), 0, parentReference.width - ac.width)
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
					xOffset = Math.floor(text[0].text.length() / 2)
					ac.width = text.collect { it.text.size() }.max() // Longest string
					// Assumes each text component is on its own line
					ac.height = text.size()
				}
				ac.x = (rpc.x / 100.0f) * parentReference.width - xOffset
				ac.y = (rpc.y / 100.0f) * parentReference.height
			}

			if (newArea) {
				entity.components << ac
				logger.debug "Added area component for {}", entity.name
			}

			logger.debug "Calculated position of {} for {}", ac, entity.name
		}
	}

	@Override
	void render(Scene scene) {
		StopWatch stopwatch = new StopWatch('rendering')

		stopwatch.start('checkScrollBoundaries')
		checkScrollBoundaries(scene)
		stopwatch.stop()

		renderDebugHints(scene, stopwatch)

		stopwatch.start('processing each entity')
		def dirtyRectangles = []
		boolean fullScreenRefresh = false
		scene.entitiesToBeRendered.each { entity ->
			if (entity instanceof PanelEntity) {
				dirtyRectangles << renderPanelBorders(entity)
			} else if (entity instanceof RerenderEntity) {
				fullScreenRefresh = true
				AreaComponent area = new AreaComponent(0, 0, viewportWidth, viewportHeight)
				dirtyRectangles << area
				terminal.clear(' ' as char, area.x, area.y, area.width, area.height)
			} else if (entity.parent) {
				TextComponent tc = entity.getComponentOfType(TextComponent)
				AreaComponent pc = entity.parent.getComponentOfType(AreaComponent)
				AreaComponent ac = translateChildToParent(entity.getComponentOfType(AreaComponent), pc)
				AreaComponent dirtyRect = translateTerminalToPixels(ac)
				dirtyRectangles << dirtyRect
				write(tc.text, ac.x, ac.y)
			} else {
				// Generic renderer
				if (isVisible(entity)) {
					Color color = entity.getComponentOfType(ColorComponent)?.color ?: AsciiPanel.white
					TextComponent tc = entity.getComponentOfType(TextComponent)
					AreaComponent panelArea = translateWorldToAsciiPanel(entity.getComponentOfType(AreaComponent), viewport)
					write(tc.text, panelArea.x, panelArea.y, color)
					AreaComponent pixelArea = translateTerminalToPixels(panelArea)
					dirtyRectangles << pixelArea
				}
			}
		}
		stopwatch.stop()

		stopwatch.start('terminal render')
		int entitiesRerendered = scene.entitiesToBeRendered.size()
		scene.entitiesToBeRendered.clear()
		if (fullScreenRefresh) {
			dirtyRectangles.clear()
			dirtyRectangles << new AreaComponent(0, 0, viewport.width * terminal.charWidth, viewport.height * terminal.charHeight)
		}
		dirtyRectangles.each { AreaComponent area ->
			terminal.paintImmediately(area.x, area.y, area.width * terminal.charWidth, area.height * terminal.charHeight)
		}
		stopwatch.stop()
		if (entitiesRerendered > 2 || dirtyRectangles.size() > 0) {
			logger.trace "Render complete, {} entities with {} dirty rectangles. {}", entitiesRerendered, dirtyRectangles.size(), stopwatch
		}
	}

	void renderDebugHints(Scene scene, StopWatch stopwatch) {
		if (scene.debug) {
			boolean pauseForHints = false
			stopwatch.start('rendering debug hints')
			scene.entitiesToBeRendered
					.findAll { !(it instanceof GaugeEntity) }
					.each { entity ->
				if (entity instanceof RerenderEntity) {
					AreaComponent area = translateWorldToAsciiPanel(entity.getComponentOfType(AreaComponent), viewport)
					logger.debug "Hinting clearing {}", area
					// Draw a red block where we're clearing (▓)
					def red = new Color(255, 0, 0, 100)
					terminal.clear((char) 178, area.x, area.y, area.width, area.height, red, Color.black)
					pauseForHints = true
				}
			}

			drawQuadtreeOutline(scene.quadtree, new Color(50, 50, 50))

			if (pauseForHints) {
				terminal.paintImmediately(0, 0, terminal.width, terminal.height)
				logger.debug "Pausing to display render hints for ${scene.debugPauseMillis} ms"
				Thread.sleep(scene.debugPauseMillis)
			}
			stopwatch.stop()
		}
	}

	void drawQuadtreeOutline(Quadtree quadtree, Color color) {
		AreaComponent bounds = translateWorldToAsciiPanel(quadtree.bounds, viewport)
		Graphics g = terminal.offscreenGraphics
		g.drawRect(bounds.x * terminal.charWidth, bounds.y * terminal.charHeight, bounds.width * terminal.charWidth, bounds.height * terminal.charHeight)
		if (quadtree.nodes[0]) {
			quadtree.nodes.each {
				drawQuadtreeOutline(it, color)
			}
		}
	}

	boolean isVisible(GameEntity entity) {
		boolean visible = false
		AreaComponent area = entity.getComponentOfType(AreaComponent)
		if (area) {
			// When detecting overlap with panels, we translate the entity's coordinates to
			// be viewport-based rather than world-based.
			AreaComponent viewportArea = translateWorldToAsciiPanel(area, viewport)
			visible = area.overlaps(viewport) && !scenePanelAreas.any { viewportArea.overlaps(it) }
		}
		return visible
	}

	/**
	 * Entities with abstract world coordinates need these translated relative
	 * to the viewport in order to get AsciiPanel rendering coordinates.
	 *
	 * @param ac abstract world coordinates
	 * @param viewport the position of the viewport in world coordinates
	 * @return an area describing the entity's screen coordinates for AsciiPanel rendering
	 */
	AreaComponent translateWorldToAsciiPanel(AreaComponent ac, AreaComponent viewport) {
		// world - viewport
		new AreaComponent(ac.x - viewport.x, ac.y - viewport.y, ac.width, ac.height)
	}

	/**
	 * Takes a cell-based area suitable for having AsciiPanel write characters
	 * and returns the pixel-based dirty rectangle required for the underlying
	 * Java2D surface to update.
	 *
	 * @param ac AsciiPanel writing coordinate
	 * @return an area describing the screen area for a Java2D render update
	 */
	AreaComponent translateTerminalToPixels(AreaComponent ac) {
		new AreaComponent(
				ac.x * terminal.charWidth,
				ac.y * terminal.charHeight,
				ac.width * terminal.charWidth,
				ac.height * terminal.charHeight)
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
				def threshX = viewport.width * 0.2
				def threshY = viewport.height * 0.2
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
					repaintScene(scene)
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
		write(s, x, y, AsciiPanel.white)
	}

	/**
	 * Writes the given string to the AsciiPanel terminal, omitting any
	 * characters which don't fit.
	 *uuu
	 * @param s the string to write to the screen
	 * @param x the terminal row to begin writing
	 * @param y the terminal column to begin writing
	 * @param color the foreground color to write with
	 */
	void write(String s, int x, int y, Color color) {
		for (int i = 0; i < s.size(); i++) {
			if (x + i >= 0 && x + i < viewport.width &&
					y >= 0 && y < viewport.height) {
				if (color.alpha != 255) {
					// If there are alpha effects happening, let's just
					// assume that we need a freshly-cleared background
					terminal.offscreenGraphics.setColor(Color.black)
					terminal.offscreenGraphics.fillRect((x + i) * terminal.charWidth, y * terminal.charHeight, terminal.charWidth, terminal.charHeight)
				}
				terminal.write(s.charAt(i), x + i, y, color)
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
	 * Renders a panel's borders. Panels' coordinates are
	 * viewport-relative,so no translation from world
	 * coordinates is necessary.
	 *
	 * Note that this does not render panel contents/inventory.
	 * That's currently handled by the main render() logic.
	 *
	 * @param panel the panel to render borders for
	 * @return the area which is being rendered
	 */
	AreaComponent renderPanelBorders(PanelEntity panel) {
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

		return translateTerminalToPixels(ac)
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
