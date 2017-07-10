package io.github.lhanson.possum

import io.github.lhanson.possum.scene.PossumSceneBuilder
import io.github.lhanson.possum.scene.Scene
import io.github.lhanson.possum.system.GameSystem
import io.github.lhanson.possum.rendering.RenderingSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MainLoop {
	@Autowired PossumSceneBuilder sceneBuilder
	@Autowired List<RenderingSystem> renderers
	@Autowired(required = false) List<GameSystem> systems

	LoopTimer timer = new LoopTimer()
	Logger log = LoggerFactory.getLogger(MainLoop)

	// How many times per second we want our simulation to update
	long SIMULATION_HZ_GOAL = 20
	// Fixed timestep representing in-game milliseconds elapsed for each simulation update
	long SIMULATION_TIMESTEP = 1000 / SIMULATION_HZ_GOAL


	void run() {
		Scene scene = sceneBuilder.getNextScene()
		while (scene) {
			timer.tick()

			timer.updateSimulation {
				scene.processInput()
				systems.each { it.update(scene, SIMULATION_TIMESTEP) }
				scene.activeInput.clear()
			}

			timer.time('Rendering') {
				renderers.each { it.render(scene) }
			}

			scene = sceneBuilder.getNextScene()
		}

		log.debug "Exiting"
		System.exit(0)
	}

	/**
	 * @return the number of frames per second currently being rendered
	 */
	long getCurrentFps() {
		timer.currentFps
	}

	/**
	 * Encapsulates the tracking and calculation of simulation and frame rendering rates
	 */
	class LoopTimer {
		// Maximum number of frames the simulation can fall behind realtime
		// before increasing SIMULATION_TIMESTEP to catch up
		final int MAX_SLOWDOWN_FRAMES = 5

		// The amount of real elapsed time since the previous frame that we need to simulate for
		long lag = 0
		// Current rendering framerate
		long currentFps = 0
		// Number of consecutive frames the simulation has lagged behind wall-clock time
		int slowdownFrames = 0

		private long previousTime = System.currentTimeMillis()
		private long fpsTimeCounter = 0
		private long fpsFrameCounter = 0

		/**
		 * Called once per main loop iteration
		 */
		void tick() {
			long currentTime = System.currentTimeMillis()
			long elapsed = currentTime - previousTime
			previousTime = currentTime
			lag += elapsed
			log.debug "Main loop - {}ms frame, {}ms lag, rendering at {} fps",
					elapsed, lag, currentFps

			// Update current FPS calculation every half-second
			fpsTimeCounter += elapsed
			fpsFrameCounter++
			if (fpsTimeCounter >= 500) {
				currentFps = fpsFrameCounter / (fpsTimeCounter / 500)
				fpsTimeCounter = 0
				fpsFrameCounter = 0
			}
		}

		/**
		 * Advances the provided fixed-timestep simulation the appropriate
		 * number of times to catch simulated game time up to the elapsed
		 * wall clock time taken by the previous frame.
		 *
		 * @param update the simulation update operation
		 */
		void updateSimulation(Closure update) {
			long updatesStartTime = System.currentTimeMillis()
			int updateIterations = 0

			while (lag >= SIMULATION_TIMESTEP) {
				update.call()
				updateIterations++
				lag -= SIMULATION_TIMESTEP
			}

			long updatesTime = System.currentTimeMillis() - updatesStartTime
			long timePerUpdate = updatesTime / (updateIterations ?: 1)
			log.debug "{} game updates took {}ms, or {}ms per update. Simulated timestep is {}ms",
					updateIterations, updatesTime, timePerUpdate, SIMULATION_TIMESTEP

			// Check to see whether our SIMULATION_HZ_GOAL is realistic, or whether
			// we have to update in larger fixed timesteps to keep up.
			if (timePerUpdate > SIMULATION_TIMESTEP) {
				log.warn "Simulation is falling behind real time"
				slowdownFrames++
				if (slowdownFrames >= MAX_SLOWDOWN_FRAMES) {
					// Simulation updates are taking longer in wall clock time than the simulation
					// steps they're supposed to represent. In order to avoid the gameplay slowing
					// down, we need to simulate larger chunks of time and/or fewer updates per loop.
					// The rendering may become slower/choppier, but the game will still be simulated accurately.
					SIMULATION_TIMESTEP *= 2
					lag = 0
					slowdownFrames = 0
					log.warn "Increasing SIMULATION_TIMESTEP goal to $SIMULATION_TIMESTEP"
				}
			} else {
				slowdownFrames = 0
			}
		}

		/**
		 * Executes the provided closure and logs its elapsed time
		 * @param description a description of the action to be used in the log message
		 * @param action the closure to invoke
		 */
		void time(String description, Closure action) {
			long startTime = System.currentTimeMillis()
			action.call()
			log.debug "$description took {}ms", System.currentTimeMillis() - startTime
		}

	}
}
