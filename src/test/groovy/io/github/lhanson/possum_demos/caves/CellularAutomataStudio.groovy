package io.github.lhanson.possum_demos.caves

import io.github.lhanson.possum.MainLoop
import io.github.lhanson.possum.collision.ImpassableComponent
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
import io.github.lhanson.possum.entity.menu.ButtonItemEntity
import io.github.lhanson.possum.entity.menu.IntegerItemEntity
import io.github.lhanson.possum.entity.menu.MenuEntity
import io.github.lhanson.possum.entity.PanelEntity
import io.github.lhanson.possum.entity.TextEntity
import io.github.lhanson.possum.input.EightWayInputContext
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
import java.awt.event.KeyEvent
import java.text.DecimalFormat

import static io.github.lhanson.possum.component.TextComponent.Modifier.*

/**
 * Demo utility for exploring the parameters of cave generation
 * using cellular automata.
 */
@SpringBootApplication(scanBasePackages = [
		'io.github.lhanson.possum'
])
class CellularAutomataStudio {
	@Autowired MainLoop mainLoop

	static void main(String[] args) {
		def context = new SpringApplicationBuilder(CellularAutomataStudio)
				.headless(false)
				.web(false)
				.run(args)
		// Run the main game loop
		context.getBean(MainLoop).run()
	}

	@Component
	class SceneBuilder extends PossumSceneBuilder {
		final String MENU = START
		final String CAVE = 'cave'
		final String QUITTING = 'quitting'
		Logger log = LoggerFactory.getLogger(this.class)
		@Autowired MovementSystem movementSystem
		@Autowired RenderingSystem renderingSystem
		@Autowired CellularAutomatonCaveGenerator caveGenerator
		@Autowired WallCarver wallCarver
		@Autowired Random random
		MenuEntity menu

		@PostConstruct
		void addScenes() {
			menu = new MenuEntity(new RelativePositionComponent(50, 50), 3, [
					new IntegerItemEntity('Width', caveGenerator.width, 0),
					new IntegerItemEntity('Height', caveGenerator.height, 0),
					new IntegerItemEntity('Smoothing Generations', 10, 0),
					new IntegerItemEntity('Initial Density', caveGenerator.initialFactor, 0),
					// TODO: these two aren't exposed yet
					//new MenuItemEntity(text: 'Birth Factor', caveGenerator.birthFactor),
					//new MenuItemEntity(text: 'Death Factor', caveGenerator.deathFactor),
					new ButtonItemEntity('Generate', {
						currentScene.uninit()
						transition(CAVE)
					}),
			])
			[menuScene, caveScene, quittingScene].each { addScene(it) }
		}


		Scene menuScene = new Scene(
				MENU,
				{[
						new TextEntity(name: 'title', components: [
								new TextComponent('-= Cellular Automata Cave Generator =-', BOLD),
								new RelativePositionComponent(50, 33) ]),
						new TextEntity('Options',
								new RelativePositionComponent(50, 40)),
						menu
				]},
				[ new EightWayInputContext(
						// keyCode handlers
						[ (KeyEvent.VK_ESCAPE) : { transition(QUITTING) }, ]
				) ],
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

		SceneInitializer caveInitializer = new SceneInitializer() {
			@Override
			List<GameEntity> initScene() {
				caveGenerator.width = menu.valueOf 'Width'
				caveGenerator.height = menu.valueOf 'Height'
				caveGenerator.initialFactor = menu.valueOf 'Initial Density'
				caveGenerator.generate(menu.valueOf('Smoothing Generations'))
				def room = caveGenerator.rooms.sort { it.size() }.last()
				def entities = wallCarver.getTiles(room)
				def floorTiles = entities.findAll { !it.getComponentOfType(ImpassableComponent) }

				AreaComponent startPos = floorTiles[(int)(floorTiles.size() / 2)].getComponentOfType(AreaComponent)

				def hero = new GameEntity(
						name: 'hero',
						components: [
								new TextComponent('@'),
								new AreaComponent(startPos),
								new VelocityComponent(0, 0),
								new AnimatedComponent(pulseDurationMillis: 1000, repeat: true),
								new PlayerInputAwareComponent(),
								new CameraFocusComponent()
						])
				entities << hero

				def rightHudPanel = new PanelEntity(name: 'rightHudPanel', padding: 1)
				def simulationHzGauge
				simulationHzGauge = new GaugeEntity(name: 'simulationHzGauge')
				simulationHzGauge.update = { ticks ->
					def simHz = (1 / ticks) * 1000
					def formatted = new DecimalFormat("#0").format(simHz)
					simulationHzGauge.text = "$formatted Hz"
				}
				def fpsGauge = new GaugeEntity(name: 'fpsGauge')
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

		Scene caveScene = new Scene(CAVE,
				caveInitializer,
				[ new EightWayInputContext(
						// keyCode handlers
						[ (KeyEvent.VK_ESCAPE): { transition(START) }, ],
						// keyChar handlers
						[ ((char) '?') : { transition(MENU, true) } ])
				],
				loadingScene
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

	}

}
