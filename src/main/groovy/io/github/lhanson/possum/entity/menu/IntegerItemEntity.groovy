package io.github.lhanson.possum.entity.menu

/** A menu item representing an integer value */
class IntegerItemEntity extends MenuItemEntity {
	Integer value

	IntegerItemEntity(String label, int value) {
		super("$label $value")
		this.label = label
		this.value = value
	}

}
