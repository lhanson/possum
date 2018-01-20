package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.PlayerInputAwareComponent
import io.github.lhanson.possum.entity.menu.MenuEntity
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.scene.Scene
import org.springframework.stereotype.Component

/**
 * Handles interactions with UI menus
 */
@Component
class MenuSystem extends GameSystem {
	String name = 'MenuSystem'

	@Override
	void doInitScene(Scene scene) {
		super.doInitScene(scene)
	}

	@Override
	void doUpdate(Scene scene, double elapsed) {
		if (scene.activeInput) {
			log.trace("Updating menu for {} active inputs", scene.activeInput.size())
			scene.getEntitiesMatching([PlayerInputAwareComponent])
					.findAll { it instanceof MenuEntity }
					.each { MenuEntity menu->
				log.trace "Applying {} to {}", scene.activeInput, menu.name
				scene.activeInput.each { input ->
					switch (input) {
						case (MappedInput.UP):
							scene.entityNeedsRendering(menu.selectedItem)
							menu.decrementSelection()
							scene.entityNeedsRendering(menu.selectedItem)
							break
						case (MappedInput.DOWN):
							scene.entityNeedsRendering(menu.selectedItem)
							menu.incrementSelection()
							scene.entityNeedsRendering(menu.selectedItem)
							break
					}
				}
			}
		}
	}

}
