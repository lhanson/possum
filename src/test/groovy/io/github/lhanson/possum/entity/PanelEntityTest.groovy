package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.RelativeWidthComponent
import io.github.lhanson.possum.component.TextComponent
import spock.lang.Specification

class PanelEntityTest extends Specification {
	def text = 'Panel Text'

	def "UI Panels need an area and an inventory to make much sense, so defaults are created"() {
		when:
			def panel = new PanelEntity(name: 'panel')
			panel.init()
		then:
			panel.getComponentOfType(AreaComponent)
			panel.getComponentOfType(InventoryComponent)
	}

	def "Panel without a specified area should shrink-wrap around a nested entity"() {
		given:
			def panel = new PanelEntity(name: 'panel')
			def panelText = new TextEntity(name: 'text', parent: panel, components: [new TextComponent(text)])
			panel.components.add(new InventoryComponent([panelText]))
			panel.init()
		when:
			AreaComponent panelArea = panel.getComponentOfType(AreaComponent)
			AreaComponent textArea = panelText.getComponentOfType(AreaComponent)
		then:
			textArea.height == 1
			textArea.width == text.length()
			panelArea.width == textArea.width + panel.padding * 2
			panelArea.height == textArea.height + panel.padding * 2
	}

	def "Panel without a specified area should shrink-wrap around a nested entity with padding"() {
		given:
			def panel = new PanelEntity(name: 'panel', padding: 10)
			def panelText1 = new TextEntity(name: 'text', parent: panel, components: [new TextComponent(text)])
			def panelText2 = new TextEntity(name: 'text', parent: panel, components: [new TextComponent(text)])
			panel.components.add(new InventoryComponent([panelText1, panelText2]))
			panel.init()
		when:
			AreaComponent textArea1 = panelText1.getComponentOfType(AreaComponent)
			AreaComponent textArea2 = panelText2.getComponentOfType(AreaComponent)
		then:
			textArea1.x == panel.padding
			textArea1.y == panel.padding
			textArea2.x == panel.padding
			textArea2.y == panel.padding + 1
	}

	def "Panel without a specified area should shrink-wrap around multiple nested entities"() {
		given:
			def panel = new PanelEntity(name: 'panel')
			def child1 = new TextEntity(parent: panel, components: [new TextComponent(text)])
			def child2 = new TextEntity(parent: panel, components: [new TextComponent(text * 2)])
			panel.components.add(new InventoryComponent([child1, child2]))
			panel.init()
		when:
			AreaComponent panelArea = panel.getComponentOfType(AreaComponent)
		then:
			panelArea.width == (text.length() * 2) + (panel.padding * 2)
			panelArea.height == 2 + (panel.padding * 2)
	}

	def "Panel without a specified area should shrink-wrap around multiple nested entities and respect padding"() {
		given:
			def panel = new PanelEntity(name: 'panel', padding: 1)
			def child1 = new TextEntity(parent: panel, components: [new TextComponent(text)])
			def child2 = new TextEntity(parent: panel, components: [new TextComponent(text * 2)])
			panel.components.add(new InventoryComponent([child1, child2]))
			panel.init()
		when:
			AreaComponent panelArea = panel.getComponentOfType(AreaComponent)
		then:
			// One unit of padding on each side
			panelArea.width == (text.length() * 2) + 2
			panelArea.height == 2 + 2
	}

	def "Panels with a relative width specified but no area should have a default relative area added"() {
		given:
			def panel = new PanelEntity( components: [new RelativeWidthComponent(50)])
		when:
			panel.init()
		then:
			panel.getComponentOfType(RelativePositionComponent)
			panel.getComponentOfType(AreaComponent)
	}

}
