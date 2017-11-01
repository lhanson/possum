package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.AnimatedComponent
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.ColorComponent
import io.github.lhanson.possum.scene.Scene
import org.springframework.stereotype.Component

import java.awt.Color

@Component
class AnimationSystem extends GameSystem {
	String name = 'AnimationSystem'

	@Override
	void doUpdate(Scene scene, double elapsed) {
		scene.getEntitiesMatching([AnimatedComponent]).each { entity ->
			AnimatedComponent ac = entity.getComponentOfType(AnimatedComponent)
			ColorComponent cc = entity.getComponentOfType(ColorComponent)

			ac.currentDuration += elapsed

			if (cc == null) {
				entity.addComponent(new ColorComponent(color: new Color(255, 255, 255, 0)))
				scene.updateEntitiesByComponent()
			} else {
				cc.color = new Color(cc.color.red, cc.color.green, cc.color.blue, getPulsedAlpha(ac))
			}
			if (ac.currentDuration >= ac.pulseDurationMillis) {
				// Pulse cycle complete, either repeat or stop
				if (ac.repeat) {
					ac.currentDuration = ac.currentDuration - ac.pulseDurationMillis
				} else {
					entity.removeComponent(ac)
					scene.updateEntitiesByComponent()
				}
			}
			scene.entityNeedsRendering(entity, entity.getComponentOfType(AreaComponent))
		}
	}

	/**
	 * Calculates the alpha value for a given {@link AnimatedComponent} based on
	 * where the current duration is within the total pulse duration. The alpha
	 * value will range from opaque to transparent and then back to opaque within
	 * a single cycle.
	 *
	 * @param ac the animation component to calculate the alpha value for
	 * @return an alpha value between 0 and 255 inclusive
	 */
	int getPulsedAlpha(AnimatedComponent ac) {
		if (ac.currentDuration < ac.pulseDurationMillis / 2) {
			// For the first half of the pulse we're decreasing alpha
			return 255 - (255 * (ac.currentDuration / ac.pulseDurationMillis))
		} else {
			// Second half of the pulse ramps back up to 255
			return 255 * (ac.currentDuration / ac.pulseDurationMillis)
		}
	}
}
