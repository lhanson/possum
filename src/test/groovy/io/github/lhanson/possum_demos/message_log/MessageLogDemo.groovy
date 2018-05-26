package io.github.lhanson.possum_demos.message_log

import io.github.lhanson.possum.MainLoop
import io.github.lhanson.possum.collision.ImpassableComponent
import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.CameraFocusComponent
import io.github.lhanson.possum.component.InventoryComponent
import io.github.lhanson.possum.component.PlayerInputAwareComponent
import io.github.lhanson.possum.component.layout.RelativeAreaComponent
import io.github.lhanson.possum.component.layout.RelativePositionComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.component.TimerComponent
import io.github.lhanson.possum.component.VelocityComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.entity.TextEntity
import io.github.lhanson.possum.entity.message_log.MessageLogPanel
import io.github.lhanson.possum.events.CollisionEvent
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.events.Subscription
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

/**
 * Demo showing message log functionality
 */
@SpringBootApplication(scanBasePackages = ['io.github.lhanson.possum', 'io.github.lhanson.possum_demos.message_log'])
class MessageLogDemo {
	@Autowired MainLoop mainLoop

	static void main(String[] args) {
		def context = new SpringApplicationBuilder(MessageLogDemo)
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
		Logger log = LoggerFactory.getLogger(this.class)
		@Autowired MovementSystem movementSystem
		@Autowired RenderingSystem renderingSystem
		@Autowired CellularAutomatonCaveGenerator caveGenerator
		@Autowired WallCarver wallCarver
		@Autowired Random random
		@Autowired EventBroker eventBroker
		MessageLogPanel messageLogPanel

		@PostConstruct
		void addScenes() {
			[startScene, caveScene, quittingScene].each {
				addScene(it)
			}
			eventBroker.subscribe(this)
		}

		Scene startScene = new Scene(
				START,
				{[
						new TextEntity('Mesage Log Demo',
								new RelativePositionComponent(50, 50)),
						new TextEntity('-- press [enter] to start, [esc] or [q] to quit --',
								new RelativePositionComponent( 50, 90))
				]},
				[ new EightWayInputContext(
						// keyCode handlers
						[ (KeyEvent.VK_ESCAPE): { transition(QUITTING) },
						  (KeyEvent.VK_Q):      { transition(QUITTING) },
						  (KeyEvent.VK_ENTER) : { transition(CAVE) }]) ],
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
				caveGenerator.width = 40
				caveGenerator.height = 40
				caveGenerator.initialFactor = 50
				caveGenerator.generate(10)
				def room = caveGenerator.rooms.sort { it.size() }.last()
				def entities = wallCarver.getTiles(room)
				def floorTiles = entities.findAll { !it.getComponentOfType(ImpassableComponent) }

				def startIndex = random.nextInt(floorTiles.size())
				AreaComponent startPos = floorTiles[startIndex].getComponentOfType(AreaComponent)

				def hero = new GameEntity(
						name: 'hero',
						components: [
								new TextComponent('@'),
								new AreaComponent(startPos),
								new VelocityComponent(0, 0),
								new PlayerInputAwareComponent(),
								new CameraFocusComponent()
						])
				entities << hero

				// TODO: panels should handle fixed dimensions, not always auto-stretch
				messageLogPanel = new MessageLogPanel(name: 'messageLog', padding: 1)
				messageLogPanel.components.addAll([
						new RelativePositionComponent(50, 100),
						new RelativeAreaComponent(relativeWidth: 80),
						new InventoryComponent([new TextEntity('Welcome! Bump into stuff, I guess...')])
				])
				entities << messageLogPanel

				return entities
			}
		}

		Scene caveScene = new Scene(
				CAVE,
				caveInitializer,
				[ new EightWayInputContext(
						// keyCode handlers
						[ (KeyEvent.VK_ESCAPE) : { transition(START) } ])
				],
				loadingScene
		)

		@Subscription
		// TODO: probably need an actual "collided" event vs. "these two things occupy the same square"
		// TODO: EntityMovedEvent, then a CollisionEvent
		void collisionEvent(CollisionEvent event) {
			messageLogPanel.addMessage(
					"${event.entity.name} collided with ${event.collider.name}")
		}
	}
}
