package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.scene.Scene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A GameSystem operates on a entities having {@link GameComponent}s
 * which the particular system is aware of, thereby enabling specific behaviors.
 *
 * For example, a character movement system may process all {@link GameEntity}s
 * possessing Position, Velocity, and Collider {@link GameComponent}s to facilitate
 * movement.
 */
abstract class GameSystem {
	Logger log = LoggerFactory.getLogger(this.class)

	/**
	 * @return the name of this {@code GameSystem}
	 */
	abstract String getName()

	/**
	 * Allows systems to do scene-specific initialization before
	 * a scene transition.
	 *
	 * @param scene the scene being transitioned to
	 */
	final void initScene(Scene scene) {
		log.debug("Initializing {} for {}", name, scene.id)
		doInitScene(scene)
	}

	/**
	 * For each main loop iteration, each system has the opportunity to
	 * perform some work, including processing scene entities it knows
	 * how to deal with.
	 *
	 * Calls {@code doUpdate()} on implementing classes.
	 *
	 * @param scene the currently active scene
	 * @param elapsed the amount of time since the last game update
	 */
	final void update(Scene scene, double elapsed) {
		long startTime = System.currentTimeMillis()
		doUpdate(scene, elapsed)
		long updateTime = System.currentTimeMillis() - startTime
		if (updateTime > 0) {
			log.trace("Updating {} took {} ms", name, updateTime)
		}
	}

	/**
	 * Implementation-specific method called For each main loop iteration,
	 * allows implementing system to process scene entities.
	 *
	 * @param scene the currently active scene
	 * @param elapsed the amount of time since the last game update
	 */
	abstract void doUpdate(Scene scene, double elapsed)

	/**
	 *
	 * Implementation-specific method called before a scene transition
	 * enabling systems to do scene-specific initialization.
	 *
	 * @param scene the scene being transitioned to
	 */
	void doInitScene(Scene scene) { }

}
