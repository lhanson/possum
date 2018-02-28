package io.github.lhanson.possum.rendering

import asciiPanel.AsciiPanel
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.scene.Scene
import io.github.lhanson.possum.scene.SceneBuilder
import spock.lang.Specification

class AsciiPanelRenderingSystemTest extends Specification {
	AsciiPanelRenderingSystem renderer
	Scene scene

	def setup() {
		scene = SceneBuilder.createScene()
		renderer = new AsciiPanelRenderingSystem()
		renderer.makeVisible = false // don't flash a blank JPanel during tests
		renderer.scene = scene
		renderer.init()
	}

	def "Write ignores objects outside the viewport"() {
		given:
			renderer.terminal = Mock(AsciiPanel)
		when:
			renderer.write('s', -1, -1)
			renderer.write('s', scene.viewport.width, scene.viewport.height)
		then:
			// The only other interaction should be getting default foreground color
			_ * renderer.terminal.getDefaultForegroundColor()
			0 * _
	}

	def "Track initialized scenes"() {
		given:
			Scene scene2 = SceneBuilder.createScene()
		when:
			renderer.initScene(scene)
			renderer.initScene(scene2)
			renderer.uninitScene(scene)
		then:
			renderer.runningScenes == [scene2]
	}

	def "Don't reinitialize scenes we never uninitialized"() {
		when:
			renderer.initScene(scene)
			renderer.initScene(scene)
		then:
			renderer.runningScenes == [scene]
	}

	def "Panel areas are stored and loaded by scene"() {
		given:
			PanelEntity panel = new PanelEntity(components: [new AreaComponent(0, 0, 10, 10)])
			Scene scene2 = SceneBuilder.createScene({[panel]})
		and:
			renderer.initScene(scene2)
		when:
			// This updates the renderer's scenePanelAreas for the empty scene
			renderer.initScene(scene)
		then:
			renderer.scenePanelAreas == []
	}

	def "Panel areas are reloaded with their scene"() {
		given:
			PanelEntity panel = new PanelEntity(components: [new AreaComponent(0, 0, 10, 10)])
			Scene scene2 = SceneBuilder.createScene({[panel]})
			renderer.initScene(scene2)
		and:
			// This updates the renderer's scenePanelAreas for the new, empty scene
			renderer.initScene(scene)
		when:
			renderer.initScene(scene2)
		then:
			renderer.scenePanelAreas == [panel.getComponentOfType(AreaComponent)]
	}

	def "Regular entities are obscured by panels"() {
		given:
			GameEntity hiddenEntity = new GameEntity(components: [new AreaComponent(0, 0, 0, 0)])
			GameEntity visibleEntity = new GameEntity(components: [new AreaComponent(20, 20, 0, 0)])
			PanelEntity panel = new PanelEntity(components: [new AreaComponent(0, 0, 10, 10)])
			scene.setEntities([hiddenEntity, visibleEntity, panel])
		when:
			renderer.initScene(scene)
		then:
			!renderer.isVisible(hiddenEntity)
			renderer.isVisible(visibleEntity)
			renderer.isVisible(panel)
	}

}
