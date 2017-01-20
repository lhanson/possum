package io.github.lhanson.possum.gameState

import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.PositionComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.component.TimerComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.input.Input
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

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
	private long lastTimestamp = System.currentTimeMillis()
	Logger log = LoggerFactory.getLogger(this.class)
	List<GameEntity> entities
	Mode currentMode
	Mode nextMode = MAIN_MENU
	long elapsedTicks

	/**
	 * For each iteration of the main loop, {@code GameStateSystem} has
	 * an opportunity to set up state transitions between various game modes.
	 *
	 * Systems will continue to act uniformly on whatever {@link GameEntity}s
	 * are returned, so the only real difference between one game mode (e.g., main menu)
	 * and the next (high scores) is the set of entities being acted upon.
	 * @return the list of GameEntitys active in the current mode, or
	 *         null if the game should quit
	 */
	List<GameEntity> entitiesForCurrentState() {
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
										new PositionComponent(alignment: CENTERED, y: 22)
								]
							},
					]
					break
				case PLAYING:
					entities = [
							new GameEntity() {
								String name = 'hero'
								List<GameComponent> components = [
										new TextComponent(text: '@'),
										new PositionComponent(alignment: CENTERED)
								]
							},
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
					break
				case EXIT:
					entities = null
					break
				default:
					throw new IllegalStateException("Unrecognized next state: $nextMode")
			}
			currentMode = nextMode
		}

		// Update elapsed ticks
		long now = System.currentTimeMillis()
		elapsedTicks = now - lastTimestamp
		lastTimestamp = now

		return entities
	}

	/**
	 * Receives messages regarding mode changes, which will be
	 * enacted upon the next iteration of the main loop.
	 * @param nextMode the mode which should be transitioned to
	 */
	@EventListener
	void modeChange(Mode nextMode) {
		log.debug "Received modeChange message: $nextMode"
		this.nextMode = nextMode
	}

	@EventListener
	void input(Input input) {
		log.debug "Received input message $input"
	}
}
