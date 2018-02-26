package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.ColorComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.component.java2d.Java2DRectangleComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GaugeEntity
import io.github.lhanson.possum.entity.TextEntity
import io.github.lhanson.possum.events.EntityPreRenderEvent
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.events.Subscription
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.scene.Scene
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.awt.Color

import static io.github.lhanson.possum.component.AreaComponent.FrameOfReference.*

/**
 * System which detects a {@link MappedInput#DEBUG} event and introduces a
 * debug-hinting frame which is rendered between normal frames. This hinting
 * frame will visually highlight areas of the screen which are about to be
 * re-rendered, and will pause briefly while highlighting.
 */
@Component
class DebugSystem extends GameSystem {
	@Autowired EventBroker eventBroker
	String name = 'DebugSystem'
	static final String QuadtreeOutlineName = 'QuadtreeOutline'
	/** The entities being specifically managed by the DebugSystem (rendering hints, etc.) */
	Map<String, List<GameEntity>> debugEntities = [:]
	/** Contains scene IDs for which debugging is enabled */
	Set<String> debugEnabled = [] as Set
	/** The number of milliseconds to pause after rendering debug hints for a given Scene ID **/
	Map<String, Integer> debugPause = [:]
	/** Contains Scene IDs needing to pause to display rendering hints */
	Set<String> pauseFor = [] as Set

	private Color renderHintColor = new Color(255, 0, 0, 100)
	private String renderHint = String.valueOf((char) 178) // ▓

	@Override
	void doInitScene(Scene scene) {
		eventBroker.subscribe(this)
		debugEntities[scene.id] = []
		debugPause[scene.id] = 200
	}

	@Override
	void doUninitScene(Scene scene) {
		eventBroker.unsubscribe(this)
		debugEntities.remove(scene.id)
		debugEnabled.remove(scene.id)
		debugPause.remove(scene.id)
		pauseFor.remove(scene.id)
	}

	@Override
	void doUpdate(Scene scene, double elapsed) {
		scene.activeInput.each { input ->
			switch (input) {
				case (MappedInput.DEBUG):
					toggleDebug(scene)
					break
				case (MappedInput.INCREASE_DEBUG_PAUSE):
					debugPause[scene.id] += 200
					log.debug "Increased debug pause to ${debugPause[scene.id]}"
					break
				case (MappedInput.DECREASE_DEBUG_PAUSE):
					if (debugPause[scene.id] >= 200) {
						debugPause[scene.id] -= 200
						log.debug "Decreased debug pause to ${debugPause[scene.id]}"
					} else {
						log.debug "Can't decrease debug pause any further (${debugPause[scene.id]})"
					}
					break
			}
		}
	}

	@Subscription
	void entityPreRender(EntityPreRenderEvent event) {
		// If the scene is in debug mode and we haven't already processed this
		// entity in this frame, create render hints
		GameEntity entity = event.entity
		if (debugEnabled.contains(entity.scene?.id) && !event.entity.scene.queuedForRendering(event.entity) ) {
			log.debug "Rendering debug hints for entity {}", entity
			AreaComponent previousArea = event.previousArea
			AreaComponent area = entity.getComponentOfType(AreaComponent)
			if (area.frameOfReference in [WORLD, PARENT] && entity.getComponentOfType(TextComponent) && !(entity instanceof GaugeEntity)) {
				// A text entity in WORLD or PARENT reference
				log.debug "Hinting render at {}", area
				if (area.frameOfReference == PARENT) {
					area = entity.scene.translateChildToParent(area, entity.parent.getComponentOfType(AreaComponent))
				}
				GameEntity debugHint = new TextEntity(
						name: "Debug hint for ${entity.name}",
						parent: entity.parent,
						components: [ new TextComponent(renderHint), new ColorComponent(renderHintColor), area])
				entity.scene.debugEntityNeedsRendering(debugHint)
				if (previousArea) {
					// Render a hint where the entity used to be, which will be cleared next frame
					if (previousArea.frameOfReference == PARENT) {
						previousArea = entity.scene.translateChildToParent(previousArea, entity.parent.getComponentOfType(AreaComponent))
					}
					GameEntity previousAreaHint = new TextEntity(
							name: "Debug hint for ${entity.name}'s previous location",
							parent: entity.parent,
							components: [ new TextComponent(renderHint), new ColorComponent(renderHintColor), previousArea])
					entity.scene.debugEntityNeedsRendering(previousAreaHint)
				}
			} else {
				// An entity already in ASCII_PANEL or pixel coordinates, pass a hint directly to rendering queue
				GameEntity debugHint = new TextEntity(
						name: "Debug hint for ${entity.name}",
						parent: entity.parent,
						components: [
								new TextComponent(String.valueOf((char) 178) * area.width),
								new ColorComponent(renderHintColor),
								area])
				entity.scene.debugEntityNeedsRendering(debugHint)
			}

			// Repaint the quadtree outlines where they may have been obscured.
			// NOTE: this paints every node from the root down to the lowest level
			// the entity is contained in, but we're not too worried about efficiency here.
			// ALSO NOTE: This isn't perfect yet, when an entity moves away from a boundary
			// edge, the boundary may be painted and then overwritten shortly after by
			// whatever was behind the entity (e.g., a floor tile). Good enough for now.
			if (entity.name != QuadtreeOutlineName) {
				entity.scene.quadtree.getAllNodeBoundsWithin(area)
						.collect { AreaComponent ac ->
					entity.scene.debugEntityNeedsRendering(
							new GameEntity(name: QuadtreeOutlineName,
									components: [ac, new Java2DRectangleComponent(),
									             new ColorComponent(Color.RED)]))
				}
			}

			pauseFor.add(event.entity.scene.id)
		}
	}

	void toggleDebug(Scene scene) {
		Boolean debug = debugEnabled.contains(scene.id)
		if (debug) {
			debugEnabled.remove(scene.id)
			debugEntities[scene.id].clear()
		} else {
			debugEnabled.add(scene.id)
			def nodeBoundaryEntities = scene.quadtree
					.getAllNodeBoundsWithin(scene.viewport)
					.collect { AreaComponent ac ->
						new GameEntity(name: QuadtreeOutlineName, components: [ac, new Java2DRectangleComponent(), new ColorComponent(Color.RED)])
					}
			debugEntities[scene.id].addAll(nodeBoundaryEntities)
			debugEntities[scene.id].each { scene.debugEntityNeedsRendering(it) }
		}
		log.debug "Toggled debug mode to ${debugEnabled.contains(scene.id)}"
	}

}
