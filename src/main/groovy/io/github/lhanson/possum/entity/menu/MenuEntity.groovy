package io.github.lhanson.possum.entity.menu

import io.github.lhanson.possum.component.PlayerInputAwareComponent
import io.github.lhanson.possum.component.layout.PaddingComponent
import io.github.lhanson.possum.component.layout.RelativePositionComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.input.MappedInput

import static io.github.lhanson.possum.component.TextComponent.Modifier.*

/**
 * An extension of a UI panel, a Menu contains selectable items.
 */
class MenuEntity extends PanelEntity {
	List<MenuItemEntity> items
	int selectedItemIndex = 0

	MenuEntity() {
		this(null, [])
	}

	MenuEntity(List<MenuItemEntity> menuItems) {
		this(null, menuItems)
	}

	MenuEntity(RelativePositionComponent rpc, List<MenuItemEntity> menuItems) {
		this(rpc, null, menuItems)
	}

	MenuEntity(RelativePositionComponent rpc, Integer padding, List<MenuItemEntity> menuItems) {
		super(menuItems)
		println "Constructor for menu with ${menuItems.size()} items"
		if (padding != null) {
			println "Padding"
			this.padding = new PaddingComponent(padding)
			components.add(this.padding)
		}
		if (rpc) {
			println "Adding components"
			components.add(rpc)
		}
		println "Adding player input aware component"
		components.add(new PlayerInputAwareComponent())
		items = new ArrayList(menuItems)
		selectedItemIndex = items.findIndexOf { it.selected }
		if (selectedItemIndex == -1 && items) {
			// Select first item if not otherwise specified
			selectedItemIndex = 0
		}
		if (items) {
			addBold(items[selectedItemIndex])
		}
	}

	MenuItemEntity getSelectedItem() {
		items[selectedItemIndex]
	}

	void incrementSelection() {
		removeBold(items[selectedItemIndex])
		selectedItemIndex++
		if (selectedItemIndex >= items.size()) {
			selectedItemIndex = 0
		}
		addBold(items[selectedItemIndex])
		log.trace "Incremented selection for menu $name to $selectedItemIndex"
	}

	void decrementSelection() {
		removeBold(items[selectedItemIndex])
		selectedItemIndex--
		if (selectedItemIndex < 0) {
			selectedItemIndex = items.size() - 1
		}
		addBold(items[selectedItemIndex])
		log.trace "Decremented selection for menu $name to $selectedItemIndex"
	}

	/**
	 * @param input the mapped input received
	 * @return the list of entities which have been updated
	 */
	List<GameEntity> handleInput(MappedInput input) {
		return selectedItem.handleInput(input)
	}

	/**
	 * @param label the text label of the item we want the value of
	 * @return the value associated with the specified item, or null if no such item is found
	 */
	Object valueOf(String label) {
		items.find { it.label == label }?.value
	}

	private void removeBold(MenuItemEntity menuItem) {
		TextComponent menuText = menuItem.getComponentOfType(TextComponent)
		menuText.modifiers.remove(BOLD)
	}

	private void addBold(MenuItemEntity menuItem) {
		TextComponent menuText = menuItem.getComponentOfType(TextComponent)
		menuText.modifiers.add(BOLD)
	}

}
