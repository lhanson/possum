package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.AnimatedComponent
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.ColorComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.events.ComponentAddedEvent
import io.github.lhanson.possum.events.ComponentRemovedEvent
import io.github.lhanson.possum.events.Subscription
import io.github.lhanson.possum.scene.Scene
import org.springframework.stereotype.Component

import java.awt.Color

@Component
class AnimationSystem extends GameSystem {
	String name = 'AnimationSystem'
	List<GameEntity> animatedEntities
	boolean removingEntity = false

	@Override
	void doInitScene(Scene scene) {
		scene.eventBroker.subscribe(this)
		animatedEntities =  scene.getEntitiesMatching([AnimatedComponent])
	}

	@Override
	void doUpdate(Scene scene, double elapsed) {
		animatedEntities.each { entity ->
			AnimatedComponent ac = entity.getComponentOfType(AnimatedComponent)
			ColorComponent cc = entity.getComponentOfType(ColorComponent)

			ac.currentDuration += elapsed

			if (cc == null) {
				entity.addComponent(new ColorComponent(color: new Color(255, 255, 255, 0)))
			} else {
				cc.color = new Color(cc.color.red, cc.color.green, cc.color.blue, getPulsedAlpha(ac))
			}
			if (ac.currentDuration >= ac.pulseDurationMillis) {
				// Pulse cycle complete, either repeat or stop
				if (ac.repeat) {
					ac.currentDuration = ac.currentDuration - ac.pulseDurationMillis
				} else {
					// Temporarily ignore removal events since we're triggering one ourselves
					removingEntity = true
					entity.removeComponent(ac)
					removingEntity = false
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
			// Second half of the pulse ramps back up to 255. Note that if we're  called after
			// more time than a pulse width between updates, we need to wrap the value.
			int alpha = 255 * (ac.currentDuration / ac.pulseDurationMillis)
			if (alpha > 255) {
				alpha = alpha % 255
			}
			return alpha
		}
	}

	@Subscription
	void componentAdded(ComponentAddedEvent event) {
		if (event.component instanceof AnimatedComponent) {
			log.debug("Added {} to animated lookup list", event.entity)
			animatedEntities.add(event.entity)
		}
	}

	@Subscription
	void componentRemoved(ComponentRemovedEvent event) {
		if (!removingEntity && event.component instanceof AnimatedComponent) {
			animatedEntities.remove(event.entity)
		}
		log.debug("Removed {} from animated lookup list", event.entity)
	}

}
