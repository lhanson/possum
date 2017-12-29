package io.github.lhanson.possum_demos.caves

import io.github.lhanson.possum.MainLoop
import io.github.lhanson.possum.collision.CollisionHandlingComponent
import io.github.lhanson.possum.component.AnimatedComponent
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.CameraFocusComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.PlayerInputAwareComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.RelativeWidthComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.component.TimerComponent
import io.github.lhanson.possum.component.VelocityComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.GaugeEntity
import io.github.lhanson.possum.entity.GridEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.TextEntity
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.input.InputContext
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.rendering.RenderingSystem
import io.github.lhanson.possum.scene.PossumSceneBuilder
import io.github.lhanson.possum.scene.Scene
import io.github.lhanson.possum.scene.SceneInitializer
import io.github.lhanson.possum.system.MovementSystem
import io.github.lhanson.possum.terrain.WallCarver
import io.github.lhanson.possum.terrain.cave.CellularAutomatonCaveGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.text.DecimalFormat

/**
 * Demo showing cave generation via cellular automata.
 */
@SpringBootApplication(scanBasePackages = [
		'io.github.lhanson.possum'
])
class CellularAutomatonCaveGen {
	@Autowired MainLoop mainLoop
	@Autowired EventBroker eventBroker

	static void main(String[] args) {
		def context = new SpringApplicationBuilder(CellularAutomatonCaveGen)
				.headless(false)
				.web(false)
				.run(args)
		// Run the main game loop
		context.getBean(MainLoop).run()
	}

	@Component
	class SceneBuilder extends PossumSceneBuilder {
		final String CAVE = 'cave'
		final String QUITTING = 'quitting'
		final String WIN = 'win'
		Logger log = LoggerFactory.getLogger(this.class)
		@Autowired MovementSystem movementSystem
		@Autowired RenderingSystem renderingSystem
		@Autowired CellularAutomatonCaveGenerator caveGenerator

		@PostConstruct
		void addScenes() {
			[startScene, caveScene, quittingScene, winScene].each {
				addScene(it)
			}
		}

		Scene startScene = new Scene(
				START,
				{[
						new TextEntity(
								name: 'menuTitle',
								components: [
										new TextComponent('Main Menu'),
										new RelativePositionComponent(50, 50)
								]),
						new TextEntity(
								name: 'pressStart',
								components: [
										new TextComponent('-- press [enter] to start, [esc] to quit --'),
										new RelativePositionComponent( 50, 90)
								])
				]},
				[
						// Main menu context
						new InputContext() {
							@Override MappedInput mapInput(InputEvent rawInput) {
								if (rawInput instanceof KeyEvent) {
									switch (rawInput.keyCode) {
										case rawInput.VK_ESCAPE:
											transition(QUITTING)
											break
										case rawInput.VK_ENTER:
											transition(CAVE)
											break
									}
								}
								// Menu contexts gobble all input, none pass through
								null
							}
						}
				]
		)

		Scene loadingScene = new Scene(
				'loading',
				{
					[ new GameEntity(
							name: 'loadingText',
							components: [
									new TextComponent('Loading...'),
									new RelativePositionComponent(50, 50) ]) ]
				}
		)

		Scene quittingScene = new Scene(
				QUITTING,
				{[new GameEntity(
						name: 'quitText',
						components: [
								new TextComponent('Goodbye see you!'),
								new RelativePositionComponent(50, 50),
								new TimerComponent(ticksRemaining: 1000, alarm: { transition(null) })
						])
				]},
		)

		SceneInitializer caveInitializer = new SceneInitializer() {
			@Override
			List<GameEntity> initScene() {
				caveGenerator.width = 300
				caveGenerator.height = 300
				caveGenerator.initialFactor = 50
				GridEntity grid = caveGenerator.generate(10)
				def entities = WallCarver.getWalls(grid, (char) 219)

				AreaComponent startPos = movementSystem.randomPassableSpaceWithin(entities)
				AreaComponent finishPos = movementSystem.randomPassableSpaceWithin(entities)
				while (finishPos == startPos) {
					log.warn "Random finish position is same as start, recalculating"
					finishPos = movementSystem.randomPassableSpaceWithin(entities)
				}

				def hero = new GameEntity(
						name: 'hero',
						components: [
								new TextComponent('@'),
								startPos,
								new VelocityComponent(0, 0),
								new AnimatedComponent(pulseDurationMillis: 1000, repeat: true),
								new PlayerInputAwareComponent(),
								new CameraFocusComponent()
						])
				entities << hero

				entities << new GameEntity(
						name: 'finish',
						components: [
								new TextComponent('>'),
								finishPos,
								new CollisionHandlingComponent() {
									@Override void handleCollision(GameEntity entity) {
										if (entity == hero) {
											transition(WIN)
										}
									}
								}
						])

				def leftHudPanel = new PanelEntity(name: 'leftHudPanel', padding: 1)
				def playerPositionGauge = new GaugeEntity(
						name: 'playerPositionGauge',
						parent: leftHudPanel
				)
				playerPositionGauge.update = { ticks ->
					AreaComponent ac = hero.getComponentOfType(AreaComponent)
					playerPositionGauge.text = "${ac.position}"
				}
				leftHudPanel.components.addAll([
						new RelativePositionComponent(0, 100),
						new RelativeWidthComponent(80),
						new InventoryComponent([playerPositionGauge])
				])
				entities << leftHudPanel

				def rightHudPanel = new PanelEntity(name: 'rightHudPanel', padding: 1)
				def simulationHzGauge
				simulationHzGauge = new GaugeEntity(
						name: 'simulationHzGauge',
						parent: rightHudPanel
				)
				simulationHzGauge.update = { ticks ->
					def simHz = (1 / ticks) * 1000
					def formatted = new DecimalFormat("#0").format(simHz)
					simulationHzGauge.text = "$formatted Hz"
				}
				def fpsGauge = new GaugeEntity(
						name: 'fpsGauge',
						parent: rightHudPanel
				)
				fpsGauge.components << new AreaComponent(0, 1, 0, 1)
				fpsGauge.update = {
					// The number of ticks being simulated doesn't reflect rendering
					// frequency, so we get the actual timing from the main loop.
					def fps = mainLoop.currentFps
					def formatted = new DecimalFormat("#0").format(fps)
					fpsGauge.text = "$formatted fps"
				}
				rightHudPanel.components.addAll([
						new RelativePositionComponent(100, 100),
						new RelativeWidthComponent(20),
						new InventoryComponent([simulationHzGauge, fpsGauge])
				])
				entities << rightHudPanel

				return entities
			}
		}

		Scene caveScene = new Scene(
				CAVE,
				caveInitializer,
				[
				  new InputContext() {
					  @Override
					  MappedInput mapInput(InputEvent rawInput) {
						  if (rawInput instanceof KeyEvent) {
							  switch (rawInput.keyCode) {
								  case rawInput.VK_UP:
								  case rawInput.VK_K:
									  return MappedInput.UP
								  case rawInput.VK_DOWN:
								  case rawInput.VK_J:
									  return MappedInput.DOWN
								  case rawInput.VK_LEFT:
								  case rawInput.VK_H:
									  return MappedInput.LEFT
								  case rawInput.VK_RIGHT:
								  case rawInput.VK_L:
									  return MappedInput.RIGHT
								  case rawInput.VK_PLUS:
								  case rawInput.VK_ADD:
								  case rawInput.VK_SHIFT | rawInput.VK_EQUALS:
									  return MappedInput.INCREASE_DEBUG_PAUSE
								  case rawInput.VK_MINUS:
									  return MappedInput.DECREASE_DEBUG_PAUSE
								  case rawInput.VK_ESCAPE:
									  transition(START)
									  break
								  case rawInput.VK_D:
									  return MappedInput.DEBUG
									  break
								  case rawInput.VK_P:
									  return MappedInput.PAUSE
									  break
							  }
						  }
						  null
					  }
				  }
				],
				loadingScene
		)

		Scene winScene = new Scene(
				WIN,
				{[
						new GameEntity(
								name: 'winText',
								components: [
										new TextComponent('Congrats, you won!'),
										new RelativePositionComponent(50, 50),
										new TimerComponent(ticksRemaining: 1000, alarm: { transition(START) })
								])
				]}
		)
	}

}
