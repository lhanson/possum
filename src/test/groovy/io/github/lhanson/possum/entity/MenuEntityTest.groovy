package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.PlayerInputAwareComponent
import io.github.lhanson.possum.component.layout.RelativePositionComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.menu.IntegerItemEntity
import io.github.lhanson.possum.entity.menu.MenuEntity
import io.github.lhanson.possum.entity.menu.MenuItemEntity
import spock.lang.Specification

class MenuEntityTest extends Specification {

	def "Shorthand constructor sets up inventory items and components"() {
		when:
			MenuEntity menu = new MenuEntity(
					new RelativePositionComponent(50, 50),
					[new MenuItemEntity(), new MenuItemEntity()])
		then:
			menu.getComponentOfType(InventoryComponent).size() == 2
			menu.getComponentOfType(RelativePositionComponent)
	}

	def "A menu calculates its bounds correctly"() {
		given:
			MenuEntity menu = new MenuEntity([
					new MenuItemEntity(text: 'Option 1'),
					new MenuItemEntity(text: 'Option 2'),
					new MenuItemEntity(text: 'Option 3'),
			])
		when:
			AreaComponent ac = menu.getComponentOfType(AreaComponent)
		then:
			ac.height == 3 + menu.padding.height
			ac.width == 'Option X'.length() + menu.padding.width
	}

	def "Menus compute inventories just like panels do"() {
		given:
			def menuItems = [
					new MenuItemEntity(text: 'Width'),
					new MenuItemEntity(text: 'Height'),
					new MenuItemEntity(text: 'Smoothing Generations'),
					new MenuItemEntity(text: 'Initial Density'),
					new MenuItemEntity(text: 'Birth Factor'),
					new MenuItemEntity(text: 'Death Factor'),
					new MenuItemEntity(text: 'Generate'),
			]
			MenuEntity menu = new MenuEntity(new RelativePositionComponent(50, 50), menuItems)
		when:
			AreaComponent area = menu.getComponentOfType(AreaComponent)
		then:
			area.height == menuItems.size() + menu.padding.height
	}

	def "Menus come with a PlayerInputAwareComponent by default"() {
		when:
			MenuEntity menu = new MenuEntity()
		then:
			menu.getComponentOfType(PlayerInputAwareComponent)
	}

	def "Menus track an index of the selected item"() {
		when:
			MenuEntity menu = new MenuEntity([new MenuItemEntity(), new MenuItemEntity(selected: true)])
		then:
			menu.selectedItemIndex == 1
	}

	def "First item is selected by default if not otherwise specified"() {
		when:
			MenuEntity menu = new MenuEntity([new MenuItemEntity(), new MenuItemEntity()])
		then:
			menu.selectedItemIndex == 0
	}

	def "Selected item index for menus without entities"() {
		when:
			MenuEntity menu = new MenuEntity()
		then:
			menu.selectedItemIndex == -1
	}

	def "Item selection can be incremented, and wraps around"() {
		when:
			MenuEntity menu = new MenuEntity([new MenuItemEntity(), new MenuItemEntity()])
		then:
			menu.selectedItemIndex == 0

		when:
			menu.incrementSelection()
		then:
			menu.selectedItemIndex == 1

		when:
			menu.incrementSelection()
		then:
			menu.selectedItemIndex == 0
	}

	def "Item selection can be decremented, and wraps around"() {
		when:
			MenuEntity menu = new MenuEntity([new MenuItemEntity(), new MenuItemEntity()])
		then:
			menu.selectedItemIndex == 0

		when:
			menu.decrementSelection()
		then:
			menu.selectedItemIndex == 1

		when:
			menu.decrementSelection()
		then:
			menu.selectedItemIndex == 0
	}

	def "Selected item has a bold text attribute added"() {
		when:
			MenuEntity menu = new MenuEntity([new MenuItemEntity(), new MenuItemEntity()])
			TextComponent menuText1 = menu.items[0].getComponentOfType(TextComponent)
			TextComponent menuText2 = menu.items[1].getComponentOfType(TextComponent)
		then:
			menuText1.modifiers.contains(TextComponent.Modifier.BOLD)
			!menuText2.modifiers.contains(TextComponent.Modifier.BOLD)

		when:
			menu.incrementSelection()
		then:
			!menuText1.modifiers.contains(TextComponent.Modifier.BOLD)
			menuText2.modifiers.contains(TextComponent.Modifier.BOLD)
	}

	def "Menu item values are right-justified according to panel size"() {
		given:
			MenuEntity menu = new MenuEntity([new IntegerItemEntity('Label', 100)])
			AreaComponent area = menu.getComponentOfType(AreaComponent)
		when:
			area.width = 100
			TextComponent tc = menu.items[0].getComponentOfType(TextComponent)
		then:
			tc.text.length() == area.width - menu.padding.width
			tc.text.startsWith('Label')
			tc.text.endsWith('100')
	}

	def "Menu item right-justification adjusts with changing values"() {
		given:
			IntegerItemEntity item = new IntegerItemEntity('Label', 10)
			MenuEntity menu = new MenuEntity([item])
			AreaComponent area = menu.getComponentOfType(AreaComponent)
		when:
			area.width = 100
			TextComponent tc = item.getComponentOfType(TextComponent)
			int length = tc.text.length()
		then:
			length == area.width - menu.padding.width
			tc.text.startsWith('Label')
			tc.text.endsWith('10')

		when: 'Length of the value is decreased'
			item.value = 9
		then: 'Overall padded length should remain the same'
			tc.text.length() == length
	}

	def "Values can be looked up directly by their labels"() {
		when:
			MenuEntity menu = new MenuEntity([new IntegerItemEntity('Width', 10)])
		then:
			menu.valueOf('Width') == 10
			menu.valueOf('Nonexistent') == null
	}

}
