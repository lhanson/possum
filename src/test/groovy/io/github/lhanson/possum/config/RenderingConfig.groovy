package io.github.lhanson.possum.config

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.system.RenderingSystem
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RenderingConfig {
	@Bean
	RenderingSystem testRenderer() {
		new RenderingSystem() {
			@Override
			void render(List<GameEntity> entities) {
				throw new IllegalStateException("Test bean not expected to invoke no-op rendering system")
			}
		}
	}
}
