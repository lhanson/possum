package io.github.lhanson.possum.rendering

import asciiPanel.AsciiPanel
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.RelativeWidthComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.TextEntity
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.scene.Scene
import io.github.lhanson.spring.TestApplicationContextLoader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification


@ContextConfiguration(classes = AsciiPanelRenderingSystem, loader = TestApplicationContextLoader)
class AsciiPanelRenderingSystemTest extends Specification {
	@Autowired AsciiPanelRenderingSystem renderer
	Scene scene

	def setup() {
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
			menuItem.parent = panel
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
			menuItem.parent = panel
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
			def textEntity = new TextEntity(parent: panelEntity,
					components: [new TextComponent('test text')])
			panelEntity.components.add(new InventoryComponent([textEntity]))
			[panelEntity, textEntity].each { it.init() }
			scene.addEntity(panelEntity)
		when:
			renderer.resolveRelativePositions(scene)
			AreaComponent panelArea = panelEntity.getComponentOfType(AreaComponent)
		then:
			panelArea.width == renderer.viewportWidth / 2
	}

}
