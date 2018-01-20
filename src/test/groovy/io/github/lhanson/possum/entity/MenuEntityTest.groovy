package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.PlayerInputAwareComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.TextComponent
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
			menu.getComponentOfType(InventoryComponent).inventory.size() == 2
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
			ac.height == 3 + (menu.padding * 2)
			ac.width == 'Option X'.length() + (menu.padding * 2)
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
			int padding = 3
			MenuEntity menu = new MenuEntity(new RelativePositionComponent(50, 50), padding, menuItems)
		when:
			menu.sceneInitialized(null)
			AreaComponent area = menu.getComponentOfType(AreaComponent)
		then:
			area.height == menuItems.size() + (padding * 2)
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

}
