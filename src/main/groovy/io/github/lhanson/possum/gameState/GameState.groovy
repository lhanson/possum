package io.github.lhanson.possum.gameState

import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.input.InputContext
import io.github.lhanson.possum.input.InputSystem
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.input.RawInput
import mikera.vectorz.Vector2
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import java.text.DecimalFormat

import static io.github.lhanson.possum.component.Alignment.CENTERED
import static io.github.lhanson.possum.gameState.Mode.*

/**
 * Controls state transitions within the game. For example, transitions from
 * an intro screen to main menu, menu to game level, game to pause screen, etc.
 *
 * This is a special {@link io.github.lhanson.possum.system.GameSystem} which
 * is consulted at the beginning of each main loop iteration.
 */
@Component
class GameState {
	private Logger log = LoggerFactory.getLogger(this.class)
	private long lastTimestamp = System.currentTimeMillis()
	private List<GameEntity> entities
	private Mode currentMode
	private Mode nextMode = MAIN_MENU

	List<InputContext> inputContexts
	List<MappedInput> activeInput
	long elapsedTicks

	/**
	 * For each iteration of the main loop, {@code GameStateSystem} has
	 * an opportunity to set up state transitions between various game modes.
	 *
	 * // TODO: review docs
	 * Systems will continue to act uniformly on whatever {@link GameEntity}s
	 * are returned, so the only real difference between one game mode (e.g., main menu)
	 * and the next (high scores) is the set of entities being acted upon.
	 * @return the list of GameEntitys active in the current mode, or
	 *         null if the game should quit
	 */
	List<GameEntity> update(InputSystem inputSystem) {
		// Update elapsed ticks
		long now = System.currentTimeMillis()
		elapsedTicks = now - lastTimestamp
		lastTimestamp = now
//		lag += elapsed;

		log.debug "Elapsed ms: $elapsedTicks"

		// Handle mode changes
		if (nextMode != currentMode) {
			switch (nextMode) {
				case MAIN_MENU:
					entities = [
							new GameEntity() {
								String name = 'menuTitle'
								List<GameComponent> components = [
										new TextComponent(text: 'Main Menu'),
										new PositionComponent(alignment: CENTERED)
								]
							},
							new GameEntity() {
								String name = 'pressStart'
								List<GameComponent> components = [
										new TextComponent(text: '-- press [enter] to start, [esc] to quit --'),
										new PositionComponent(
												// TODO: how do we get away with not knowing the dimensions of the world here?
												// TODO: and how can 'centered' mean anything in that case?
												alignment: CENTERED,
												vector2: Vector2.of(10, 22))
								]
							},
					]
					inputContexts = [
							// Main menu context
							new InputContext() {
								@Override
								MappedInput mapInput(RawInput rawInput) {
									// Menu contexts gobble all input, none pass through
									switch (rawInput) {
										case RawInput.ESCAPE:
											modeChange(Mode.QUITTING)
											break
										case RawInput.ENTER:
											modeChange(Mode.PLAYING)
											break
										/* No options yet, so why bother?
										case RawInput.UP:
											return MappedInput.UP
										case RawInput.DOWN:
											return MappedInput.DOWN
										case RawInput.LEFT:
											return MappedInput.LEFT
										case RawInput.RIGHT:
											return MappedInput.RIGHT
											*/
									}
								}
							}
					]
					break
				case PLAYING:
					entities = [
							new GameEntity() {
								String name = 'hero'
								List<GameComponent> components = [
										new TextComponent(text: '@'),
										// TODO: how to center?
//										new PositionComponent(alignment: CENTERED),
										new PositionComponent(vector2: Vector2.of(20, 20)),
										new VelocityComponent(vector2: Vector2.of(0, 0)),
										new FocusedComponent()
								]
							},
							new GameEntity() {
								String name = 'loopTimeDisplay'
								def textComponent = new TextComponent()
								List<GameComponent> components = [
										textComponent,
										new PositionComponent(vector2: Vector2.of(0, 0)),
										new GaugeComponent(update: {
											textComponent.text = "$elapsedTicks ms"
										})
								]
							},
							new GameEntity() {
								String name = 'fpsDisplay'
								def textComponent = new TextComponent()
								List<GameComponent> components = [
										textComponent,
										new PositionComponent(vector2: Vector2.of(10, 0)),
										new GaugeComponent(update: {
											def fps = (1 / elapsedTicks) * 1000
											def formatted = new DecimalFormat("##0.##").format(fps)
											textComponent.text = "$formatted fps"
										})
								]
							},
					]
					inputContexts = [
							// Playing context
							new InputContext() {
								@Override
								MappedInput mapInput(RawInput rawInput) {
									switch (rawInput) {
										case RawInput.UP:
											return MappedInput.UP
										case RawInput.DOWN:
											return MappedInput.DOWN
										case RawInput.LEFT:
											return MappedInput.LEFT
										case RawInput.RIGHT:
											return MappedInput.RIGHT
										case RawInput.ESCAPE:
											modeChange(Mode.MAIN_MENU)
											break
									}
								}
							}
					]
					break
				case QUITTING:
					entities = [
							new GameEntity() {
								String name = 'quitText'
								List<GameComponent> components = [
										new TextComponent(text: 'See you next time!'),
										new PositionComponent(alignment: CENTERED),
										new TimerComponent(ticksRemaining: 1000, alarm: { nextMode = EXIT })
								]
							},
					]
					inputContexts = null
					break
				case EXIT:
					entities = null
					inputContexts = null
					break
				default:
					throw new IllegalStateException("Unrecognized next state: $nextMode")
			}
			currentMode = nextMode
			// Clear pending inputs before switching modes
			activeInput?.clear()
		}

		// Collect input
		activeInput = inputSystem.processInput(inputContexts)

		return entities
	}

	/**
	 * // TODO: document, and who can call it?
	 * Receives messages regarding mode changes, which will be
	 * enacted upon the next iteration of the main loop.
	 * @param nextMode the mode which should be transitioned to
	 */
	void modeChange(Mode nextMode) {
		log.debug "Changing game mode to $nextMode"
		this.nextMode = nextMode
	}

}
