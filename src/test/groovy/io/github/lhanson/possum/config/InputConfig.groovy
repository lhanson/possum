package io.github.lhanson.possum.config

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.system.InputSystem
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InputConfig {
	@Bean
	InputSystem testInputSystem() {
		new InputSystem() {
			@Override
			void processInput(List<GameEntity> entities) {
				throw new IllegalStateException("Test bean not expected to invoke no-op input system")
			}
		}
	}
}
