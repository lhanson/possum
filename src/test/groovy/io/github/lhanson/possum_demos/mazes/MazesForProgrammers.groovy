package io.github.lhanson.possum_demos.mazes

import io.github.lhanson.possum.MainLoop
import io.github.lhanson.possum.collision.CollisionHandlingComponent
import io.github.lhanson.possum.component.CameraFocusComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.GaugeComponent
import io.github.lhanson.possum.component.PlayerInputAwareComponent
import io.github.lhanson.possum.component.PositionComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.RelativeWidthComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.component.TimerComponent
import io.github.lhanson.possum.component.VelocityComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GridEntity
import io.github.lhanson.possum.entity.MobileEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.input.InputContext
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.input.RawInput
import io.github.lhanson.possum.maze.BinaryTree
import io.github.lhanson.possum.maze.MazeCarver
import io.github.lhanson.possum.rendering.RenderingSystem
import io.github.lhanson.possum.scene.PossumSceneBuilder
import io.github.lhanson.possum.scene.Scene
import io.github.lhanson.possum.system.MovementSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.stereotype.Component

import java.text.DecimalFormat

/**
 * Demo showing maze generation from the book "Mazes for Programmers",
 * https://pragprog.com/book/jbmaze/mazes-for-programmers
 */
@SpringBootApplication(scanBasePackages = [
		'io.github.lhanson.possum',
		'io.github.lhanson.mazes'
])
class MazesForProgrammers {
	@Autowired
	MainLoop mainLoop

	static void main(String[] args) {
		def context = new SpringApplicationBuilder(MazesForProgrammers)
				.headless(false)
				.web(false)
				.run(args)
		// Run the main game loop
		context.getBean(MainLoop).run()
	}

	@Component
	class SceneBuilder extends PossumSceneBuilder {
		final String MAZE = 'maze'
		final String QUITTING = 'quitting'
		final String WIN = 'win'
		Logger log = LoggerFactory.getLogger(this.class)
		@Autowired MovementSystem movementSystem
		@Autowired RenderingSystem renderingSystem

		@Override
		void initializeScenes() {
			scenesById[PossumSceneBuilder.START] = startScene()
			scenesById[MAZE] = mazeScene()
			scenesById[QUITTING] = quittingScene()
			scenesById[WIN] = winScene()
		}

		Scene startScene() {
			new Scene(
					entities: [
							new GameEntity() {
								String name = 'menuTitle'
								List<GameComponent> components = [
										new TextComponent('Main Menu'),
										new RelativePositionComponent(50, 50)
								]
							},
							new GameEntity() {
								String name = 'pressStart'
								List<GameComponent> components = [
										new TextComponent('-- press [enter] to start, [esc] to quit --'),
										new RelativePositionComponent( 50, 90)
								]
							},
					],
					inputContexts: [
							// Main menu context
							new InputContext() {
								@Override MappedInput mapInput(RawInput rawInput) {
									// Menu contexts gobble all input, none pass through
									switch (rawInput) {
										case RawInput.ESCAPE:
											transition(QUITTING)
											break
										case RawInput.ENTER:
											transition(MAZE)
											break
									}
									null
								}
							}
					]
			)
		}

		Scene quittingScene() {
			new Scene(
					entities: [
							new GameEntity() {
								String name = 'quitText'
								List<GameComponent> components = [
										new TextComponent('Goodbye see you!'),
										new RelativePositionComponent(50, 50),
										new TimerComponent(ticksRemaining: 1000, alarm: { transition(null) })
								]
							},
					],
			)
		}

		Scene mazeScene() {
			GridEntity maze = BinaryTree.linkCells(new GridEntity(30, 20))
			def entities = []
			def walls = MazeCarver.buildWalls(maze)
			entities.addAll walls

			PositionComponent startPos = movementSystem.randomPassableSpaceWithin(walls)
			PositionComponent finishPos = movementSystem.randomPassableSpaceWithin(walls)
			while (finishPos == startPos) {
				log.warn "Random finish position is same as start, recalculating"
				finishPos = movementSystem.randomPassableSpaceWithin(walls)
			}

			def hero = new GameEntity() {
				String name = 'hero'
				List<GameComponent> components = [
						new TextComponent('@'),
						startPos,
						new VelocityComponent(0, 0),
						new PlayerInputAwareComponent(),
						new CameraFocusComponent()
				]
			}
			entities << hero

			entities << new GameEntity() {
				String name = 'finish'
				List<GameComponent> components = [
						new TextComponent('>'),
						finishPos,
						new CollisionHandlingComponent() {
							@Override void handleCollision(MobileEntity mobileEntity) {
								if (mobileEntity.entity == hero) {
									transition(WIN)
								}
							}
						}
				]
			}

			def playerPositionGauge = new GaugeComponent()
			playerPositionGauge.update = { ticks ->
				def ac = hero.getComponentOfType(PositionComponent)
				playerPositionGauge.text = "$ac"
			}
			entities << new PanelEntity() {
				String name = 'leftHudPanel'
				List<GameComponent> components = [
						new RelativePositionComponent(0, 100),
						new RelativeWidthComponent(80),
						playerPositionGauge
				]
			}

			def simulationHzGauge = new GaugeComponent()
			simulationHzGauge.update = { ticks ->
				def simHz = (1 / ticks) * 1000
				def formatted = new DecimalFormat("#0").format(simHz)
				simulationHzGauge.text = "$formatted Hz"
			}

			def fpsGauge = new GaugeComponent()
			fpsGauge.update = {
				// The number of ticks being simulated doesn't reflect rendering
				// frequency, so we get the actual timing from the main loop.
				def fps = mainLoop.currentFps
				def formatted = new DecimalFormat("#0").format(fps)
				fpsGauge.text = "$formatted fps"
			}

			entities << new PanelEntity() {
				String name = 'rightHudPanel'
				List<GameComponent> components = [
						new RelativePositionComponent(100, 100),
						new RelativeWidthComponent(20),
						simulationHzGauge,
						fpsGauge,
				]
			}

			new Scene(
					entities: entities,
					inputContexts: [
							new InputContext() {
								@Override MappedInput mapInput(RawInput rawInput) {
									// Menu contexts gobble all input, none pass through
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
											transition(START)
											break
									}
									null
								}
							}
					]
			)
		}

		Scene winScene() {
			new Scene(
					entities: [
							new GameEntity() {
								String name = 'winText'
								List<GameComponent> components = [
										new TextComponent('Congrats, you won!'),
										new RelativePositionComponent(50, 50),
										new TimerComponent(ticksRemaining: 1000, alarm: { transition(START) })
								]
							},
					],
			)
		}
	}

}
