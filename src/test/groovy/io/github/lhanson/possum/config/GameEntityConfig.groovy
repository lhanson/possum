package io.github.lhanson.possum.config

import io.github.lhanson.possum.entity.GameEntity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GameEntityConfig {
	@Bean
	GameEntity entity() {
		new GameEntity() {
			String name = 'testGameEntity'
			int id = -1
		}
	}
}
