package io.github.lhanson.possum.rendering

import asciiPanel.AsciiPanel
import io.github.lhanson.possum.collision.Quadtree
import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GaugeEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.RerenderEntity
import io.github.lhanson.possum.scene.Scene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
	Map<String, List<AreaComponent>> panelAreasBySceneId = [:]
	// Panel areas in the scene, sorted by x, y coordinates
	List<AreaComponent> scenePanelAreas
	// Scenes currently initialized and running
	List<Scene> runningScenes = []
	// Scene currently rendering
	Scene scene
	// Can be disabled for unit tests to avoid flashing an empty JFrame to the screen
	boolean makeVisible = true

	@PostConstruct
	void init() {
		logger.trace "Initializing"
		terminal = new AsciiPanel(100, 40)
		logger.debug "Created terminal with fairly arbitrary 100x40 dimensions and should probably parameterize that"

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
		this.scene = scene
		if (runningScenes.contains(scene)) {
			logger.debug "Scene was not uninitialized, skipping initialization {}", scene
			scenePanelAreas = panelAreasBySceneId[scene.id]
		} else {
			logger.debug "Initializing scene {}", scene
			long startTime = System.currentTimeMillis()

			runningScenes << scene

			// Store a list of the panel areas in the scene sorted by x,y coordinates for faster reference
			scenePanelAreas = scene.panels.findResults { it.getComponentOfType(AreaComponent) }
			scenePanelAreas.sort { a, b -> a.x <=> b.y ?: a.y <=> b.y }
			panelAreasBySceneId[scene.id] = scenePanelAreas

			setVisible(makeVisible)
			logger.debug "Renderer initialization took ${System.currentTimeMillis() - startTime} ms"
		}

		// Repaint scene
		repaintScene(scene)
		terminal.clear()
	}

	@Override
	void uninitScene(Scene scene) {
		scenePanelAreas = null
		panelAreasBySceneId.remove(scene.id)
		runningScenes.remove(scene)
	}

	/**
	 * When initially rendering a scene (or after we scroll the viewport),
	 * queue all visible entities for rendering.
	 *
	 * @param scene the scene to do a complete rerender on
	 */
	def repaintScene(Scene scene) {
		// Visible non-panel entities
		scene.findNonPanelWithin(scene.viewport)
				.findAll { isVisible(it) }
				.each { scene.entityNeedsRendering(it) }

		// Panels and their contents
		scene.panels.each {
			scene.entityNeedsRendering(it)
			InventoryComponent ic = it.getComponentOfType(InventoryComponent)
			ic.inventory.each { item -> scene.entityNeedsRendering(item) }
		}

		// Hint to the render method that we can use one large dirty rectangle
		scene.entityNeedsRendering(new RerenderEntity(name: 'fullSceneRepaintRenderer'))
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
				AreaComponent rerenderArea = entity.getComponentOfType(AreaComponent)
				if (rerenderArea) {
					terminal.clear(' ' as char, rerenderArea.x, rerenderArea.y, rerenderArea.width, rerenderArea.height)
				} else {
					fullScreenRefresh = true
					terminal.clear(' ' as char, 0, 0, scene.viewport.width, scene.viewport.height)
				}
			} else if (entity.parent) {
				TextComponent tc = entity.getComponentOfType(TextComponent)
				AreaComponent pc = entity.parent.getComponentOfType(AreaComponent)
				AreaComponent ac = scene.translateChildToParent(entity.getComponentOfType(AreaComponent), pc)
				AreaComponent dirtyRect = translateTerminalToPixels(ac)
				dirtyRectangles << dirtyRect
				write(tc, ac.x, ac.y)
			} else {
				// Generic renderer
				if (isVisible(entity)) {
					Color color = entity.getComponentOfType(ColorComponent)?.color ?: terminal.defaultForegroundColor
					TextComponent tc = entity.getComponentOfType(TextComponent)
					AreaComponent panelArea = translateWorldToAsciiPanel(entity.getComponentOfType(AreaComponent), scene.viewport)
					write(tc, panelArea.x, panelArea.y, color)
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
			dirtyRectangles << new AreaComponent(0, 0, scene.viewport.width * terminal.charWidth, scene.viewport.height * terminal.charHeight)
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
			scene.entitiesToBeRendered.findAll { !(it instanceof GaugeEntity) }.each { entity ->
				AreaComponent area = entity.getComponentOfType(AreaComponent)
				// RerenderEntities are already given in AsciiPanel coordinates, otherwise translate
				if (!(entity instanceof RerenderEntity)) {
					area = translateWorldToAsciiPanel(area, scene.viewport)
				}
				logger.debug "Hinting clearing {}", area
				// Draw a red block where we're clearing (▓)
				def red = new Color(255, 0, 0, 100)
				terminal.clear((char) 178, area.x, area.y, area.width, area.height, red, Color.black)
				pauseForHints = true
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
		AreaComponent bounds = translateTerminalToPixels(translateWorldToAsciiPanel(quadtree.bounds, scene.viewport))
		Graphics g = terminal.offscreenGraphics
		g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
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
			AreaComponent viewportArea = translateWorldToAsciiPanel(area, scene.viewport)
			visible = area.overlaps(scene.viewport) && !scenePanelAreas.any { viewportArea.overlaps(it) }
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
	 * If the focused component gets to a certain viewport threshold, we
	 * recenter the viewport on it before we render the next frame.
 	 */
	void checkScrollBoundaries(Scene scene) {
		GameEntity focusedEntity = scene.getEntityMatching([CameraFocusComponent])
		if (focusedEntity) {
			AreaComponent ac = focusedEntity.getComponentOfType(AreaComponent)
			if (ac) {
				Viewport viewport = scene.viewport
				def scrollToX
				def scrollToY
				def threshX = scene.viewport.width * 0.2
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
					viewport.centerOn(scrollTo)
					logger.debug "Scroll boundaries shifted, need rendering"
					repaintScene(scene)
				}
			}
		}
	}

	void write(io.github.lhanson.possum.component.TextComponent tc, int x, int y, Color color = null) {
		if (tc.modifiers?.contains(io.github.lhanson.possum.component.TextComponent.Modifier.BOLD)) {
			if (!color) {
				color = terminal.defaultForegroundColor
			}
			color = color.brighter().brighter()
		}
		write(tc.text, x, y, color)
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
		write(s, x, y, null)
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
		if (!color) {
			color = terminal.defaultForegroundColor
		}
		for (int i = 0; i < s.size(); i++) {
			if (x + i >= 0 && x + i < scene.viewport.width &&
					y >= 0 && y < scene.viewport.height) {
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

	/**
	 * Renders a panel's borders. Panels' coordinates are
	 * viewport-relative,so no translation from world
	 * coordinates is necessary.
	 *
	 * Note that this does not render panel contents/inventory.
	 * That's currently handled by the main render() logic.
	 *
	 * @param panel the panel to render borders for
	 * @return the area (in pixel coordinates) which is being rendered
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
		for (int i = 1; i < ac.height; i++) {
			terminal.write(v, ac.x, ac.y + i)                // Left border
			terminal.write(v, ac.x + ac.width - 1, ac.y + i) // Right border
		}
		// Bottom border
		terminal.write(ll + ("$h" * (ac.width - 2)) + lr, ac.x, ac.y + (ac.height - 1))

		return translateTerminalToPixels(ac)
	}

}
