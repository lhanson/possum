package io.github.lhanson.possum_demos.viewport

import io.github.lhanson.possum.MainLoop
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.RelativePositionComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.component.VectorComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.input.InputContext
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.input.RawInput
import io.github.lhanson.possum.scene.PossumSceneBuilder
import io.github.lhanson.possum.scene.Scene
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.stereotype.Component

/**
 * Exercises viewport rendering e.g. entities larger than the viewport
 */
@SpringBootApplication(scanBasePackages = [
		'io.github.lhanson.possum',
])
class TextTruncating {
	static void main(String[] args) {
		def context = new SpringApplicationBuilder(TextTruncating)
				.headless(false)
				.web(false)
				.run(args)
		// Run the main game loop
		context.getBean(MainLoop).run()
	}

	@Component
	class ViewportSize extends VectorComponent {
		ViewportSize() { x = 5; y = 1 }
	}

	@Component
	class SceneBuilder extends PossumSceneBuilder {
		@Override void initializeScenes() {
			scenesById[PossumSceneBuilder.START] = new Scene(
					entities: [
							new GameEntity() {
								String name = 'menuTitle'
								List<GameComponent> components = [
										new TextComponent('01234567890'),
										new RelativePositionComponent(50, 50)
								]
							}
					],
					inputContexts: [
							new InputContext() {
								@Override MappedInput mapInput(RawInput rawInput) {
									transition(null)
								}
							}
					]
			)}
		}

}
