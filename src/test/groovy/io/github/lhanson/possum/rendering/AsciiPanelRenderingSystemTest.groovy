package io.github.lhanson.possum.rendering

import asciiPanel.AsciiPanel
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.RelativeWidthComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.component.VectorComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.TextEntity
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.scene.Scene
import spock.lang.Specification


class AsciiPanelRenderingSystemTest extends Specification {
	AsciiPanelRenderingSystem renderer
	Scene scene

	def setup() {
		renderer = new AsciiPanelRenderingSystem()
		renderer.makeVisible = false // don't flash a blank JPanel during tests
		renderer.init()
		scene = new Scene('testScene')
		scene.eventBroker = new EventBroker()
		scene.init()
	}

	def "Write ignores objects outside the viewport"() {
		given:
			renderer.terminal = Mock(AsciiPanel)
		when:
			renderer.write('s', -1, -1)
			renderer.write('s', renderer.viewport.width, renderer.viewport.height)
		then:
			0 * _
	}

	def "Resolve relative positioning by adding a concrete AreaComponent"() {
		given:
			def panel = new PanelEntity(components: [new RelativePositionComponent(50, 50)])
			panel.init()
			scene.addEntity(panel)
		when:
			renderer.resolveRelativePositions(scene)
			AreaComponent panelArea = panel.getComponentOfType(AreaComponent)
		then:
			panelArea.height == 2 // At minimum it's a top border and a bottom border
			panelArea.width == panel.padding * 2
			panelArea.x == (renderer.viewport.width * 0.5) - (panelArea.width / 2)
			panelArea.y == (renderer.viewport.height * 0.5) - (panelArea.height / 2)
	}

	def "Resolve relative position of inventory elements within parent panel"() {
		given:
			def menuItem = new TextEntity(components: [
					new TextComponent('Menu text'),
					new RelativePositionComponent(50, 50),
			])
			def panel = new PanelEntity(components: [
					new AreaComponent(100, 100, 0, 40, 40),
					new InventoryComponent([menuItem])
			])
			scene.addEntity(panel)
			AreaComponent panelArea = panel.getComponentOfType(AreaComponent)
		when:
			renderer.resolveRelativePositions(scene)
			AreaComponent menuItemArea = menuItem.getComponentOfType(AreaComponent)
		then:
			// Computed area is still relative to parent, viewport resolution happens on render
			menuItemArea.x == Math.floor((panelArea.width * 0.5) - (menuItem.text.size() / 2))
			menuItemArea.y == 1
	}

	def "Resolve panels' inventory elements with padding taken into account"() {
		given:
			def menuItem = new TextEntity(components: [new TextComponent('Menu text')])
			def panel = new PanelEntity(
					padding: 10,
					components: [ new InventoryComponent([menuItem]) ])
			scene.addEntity(panel)
		when:
			renderer.resolveRelativePositions(scene)
			AreaComponent menuItemArea = menuItem.getComponentOfType(AreaComponent)
		then:
			menuItemArea.x == 10
			menuItemArea.y == 10
	}

	def "Renderer resolves relative widths"() {
		given:
			def panelEntity = new PanelEntity(components: [new RelativeWidthComponent(50)])
			def textEntity = new TextEntity(components: [new TextComponent('test text')])
			panelEntity.components.add(new InventoryComponent([textEntity]))
			[panelEntity, textEntity].each { it.init() }
			scene.addEntity(panelEntity)
		when:
			renderer.resolveRelativePositions(scene)
			AreaComponent panelArea = panelEntity.getComponentOfType(AreaComponent)
		then:
			panelArea.width == renderer.viewportWidth / 2
	}

	def "Track initialized scenes"() {
		given:
			Scene scene1 = new Scene('scene1')
			Scene scene2 = new Scene('scene2')
		when:
			renderer.initScene(scene1)
			renderer.initScene(scene2)
			renderer.uninitScene(scene1)
		then:
			renderer.runningScenes == [scene2]
	}

	def "Don't reinitialize scenes we never uninitialized"() {
		given:
			Scene scene = new Scene('scene')
		when:
			renderer.initScene(scene)
			renderer.initScene(scene)
		then:
			renderer.runningScenes == [scene]
	}

	def "Center viewport"() {
		given:
			Scene scene = new Scene('scene')
			renderer.initScene(scene)
			AreaComponent viewport = renderer.viewport
		when:
			renderer.centerViewport(new VectorComponent(100, 100))
		then:
			viewport.x == 100 - (viewport.width / 2)
			viewport.y == 100 - (viewport.height / 2)
	}

	def "Unique viewport coordinates are maintained for each scene"() {
		given:
			Scene scene1 = new Scene('scene1')
			Scene scene2 = new Scene('scene2')

		when:
			renderer.initScene(scene1)
			renderer.centerViewport(new VectorComponent(100, 100))
		and:
			renderer.initScene(scene2)

		then:
			renderer.viewport.x == 0
			renderer.viewport.y == 0
	}

	def "Panel areas are stored and loaded by scene"() {
		given:
			PanelEntity panel = new PanelEntity(components: [new AreaComponent(0, 0, 10, 10)])
			Scene scene1 = new Scene('scene1', {[panel]})
			Scene scene2 = new Scene('scene2')
			scene1.eventBroker = new EventBroker()
			scene1.init()
			scene2.eventBroker = new EventBroker()
			scene2.init()
		and:
			renderer.initScene(scene1)
		when:
			// This updates the renderer's scenePanelAreas for the new, empty scene
			renderer.initScene(scene2)
		then:
			renderer.scenePanelAreas == []
	}

	def "Panel areas are reloaded with their scene"() {
		given:
			PanelEntity panel = new PanelEntity(components: [new AreaComponent(0, 0, 10, 10)])
			Scene scene1 = new Scene('scene1', {[panel]})
			Scene scene2 = new Scene('scene2')
			scene1.eventBroker = new EventBroker()
			scene1.init()
			scene2.eventBroker = new EventBroker()
			scene2.init()
			renderer.initScene(scene1)
		and:
			// This updates the renderer's scenePanelAreas for the new, empty scene
			renderer.initScene(scene2)
		when:
			renderer.initScene(scene1)
		then:
			renderer.scenePanelAreas == [panel.getComponentOfType(AreaComponent)]
	}

	def "Entity isn't visible if covered by a panel"() {
		given:
			GameEntity entity = new GameEntity(components: [new AreaComponent(0, 0, 1, 1)])
			PanelEntity panel = new PanelEntity(components: [new AreaComponent(0, 0, 10, 10)])
			Scene scene = new Scene('testScene', {[entity, panel]})
			scene.eventBroker = new EventBroker()
			scene.init()
		when:
			renderer.initScene(scene)
		then:
			// Entity overlaps the panel and should be invisible
			!renderer.isVisible(entity)
	}

}
