package io.github.lhanson.possum.component

import io.github.lhanson.possum.entity.GameEntity
import spock.lang.Specification

import static io.github.lhanson.possum.component.AreaComponent.FrameOfReference.PARENT

class InventoryComponentTest extends Specification {

	def "Inventory constructor sets entities' frames of reference as 'PARENT'"() {
		when:
			InventoryComponent ic = new InventoryComponent([
					new GameEntity(components: [new AreaComponent(10, 10, 1, 1)])
			])
		then:
			ic.inventory.every {
				AreaComponent ac = it.getComponentOfType(AreaComponent)
				ac.frameOfReference == PARENT
			}
	}

}
