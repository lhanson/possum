package io.github.lhanson.possum.config

import io.github.lhanson.possum.input.InputAdapter
import io.github.lhanson.possum.input.RawInput
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InputConfig {
	@Bean
	InputAdapter testInputSystem() {
		new InputAdapter() {
			@Override
			List<RawInput> collectInput() {
				throw new IllegalStateException("Test bean not expected to invoke no-op input system")
			}
		}
	}
}
