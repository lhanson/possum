package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.RelativeWidthComponent
import io.github.lhanson.possum.events.EventBroker
import spock.lang.Specification

import static io.github.lhanson.possum.component.AreaComponent.FrameOfReference.ASCII_PANEL
import static io.github.lhanson.possum.component.AreaComponent.FrameOfReference.PARENT

class PanelEntityTest extends Specification {
	def text = 'Panel Text'

	def "UI Panels need an area and an inventory to make much sense, so defaults are created"() {
		when:
			def panel = new PanelEntity(name: 'panel')
		then:
			panel.getComponentOfType(AreaComponent)
			panel.getComponentOfType(InventoryComponent)
	}

	def "Shorthand constructor automatically creates an InventoryComponent"() {
		when:
			TextEntity textEntity = new TextEntity('text')
			PanelEntity panel = new PanelEntity(textEntity)
		then:
			panel.getComponentOfType(InventoryComponent).inventory.contains(textEntity)
	}

	def "Panel without a specified area should shrink-wrap around a nested entity"() {
		given:
			def panelText = new TextEntity(text)
			def panel = new PanelEntity(panelText)
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
			def panelText1 = new TextEntity(text)
			def panelText2 = new TextEntity(text + ' 2')
			def panel = new PanelEntity(name: 'panel', padding: 10, eventBroker: new EventBroker(),
					components: [new InventoryComponent([panelText1, panelText2])])
		when:
			AreaComponent textArea1 = panelText1.getComponentOfType(AreaComponent)
			AreaComponent textArea2 = panelText2.getComponentOfType(AreaComponent)
			AreaComponent panelArea = panel.getComponentOfType(AreaComponent)
		then:
			textArea1.x == panel.padding
			textArea1.y == panel.padding
			textArea2.x == panel.padding
			textArea2.y == panel.padding + 1
			panelArea.height == textArea1.height + textArea2.height + (panel.padding * 2)
			panelArea.width == Math.max(textArea1.width, textArea2.width) + (panel.padding * 2)

	}

	def "Panel without a specified area should shrink-wrap around multiple nested entities"() {
		given:
			def panel = new PanelEntity(name: 'panel')
			def child1 = new TextEntity(text)
			def child2 = new TextEntity(text * 2)
			panel.components.add(new InventoryComponent([child1, child2]))
		when:
			AreaComponent panelArea = panel.getComponentOfType(AreaComponent)
		then:
			panelArea.width == (text.length() * 2) + (panel.padding * 2)
			panelArea.height == 2 + (panel.padding * 2)
	}

	def "Panel without a specified area should shrink-wrap around multiple nested entities and respect padding"() {
		given:
			def panel = new PanelEntity(name: 'panel', padding: 1)
			def child1 = new TextEntity(text)
			def child2 = new TextEntity(text * 2)
			panel.components.add(new InventoryComponent([child1, child2]))
		when:
			AreaComponent panelArea = panel.getComponentOfType(AreaComponent)
		then:
			// One unit of padding on each side
			panelArea.width == (text.length() * 2) + 2
			panelArea.height == 2 + 2
	}

	def "Panels with a relative width specified but no area should have a default relative area added"() {
		when:
			def panel = new PanelEntity(components: [new RelativeWidthComponent(50)])
		then:
			panel.getComponentOfType(RelativePositionComponent)
			panel.getComponentOfType(AreaComponent)
	}

	def "Panel coordinates default to the AsciiPanel frame of reference"() {
		when:
			def panel = new PanelEntity()
		then:
			AreaComponent ac = panel.getComponentOfType(AreaComponent)
			ac.frameOfReference == ASCII_PANEL
	}

	def "Panels initialize their inventories with coordinates relative to the panel"() {
		when:
			def panel = new PanelEntity(name: 'panel', padding: 1)
			panel.components.add(new InventoryComponent([new TextEntity(), new TextEntity()]))
		then:
			panel.inventoryComponent.inventory.every {
				AreaComponent ac = it.getComponentOfType(AreaComponent)
				ac.frameOfReference == PARENT
				it.parent == panel
			}
	}

}
