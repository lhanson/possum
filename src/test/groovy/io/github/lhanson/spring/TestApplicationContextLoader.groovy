package io.github.lhanson.spring

import org.springframework.boot.SpringApplication
import org.springframework.boot.test.context.SpringBootContextLoader

class TestApplicationContextLoader extends SpringBootContextLoader {
	@Override
	protected SpringApplication getSpringApplication() {
		SpringApplication testApp = super.getSpringApplication()
		testApp.headless = false
		testApp
	}
}
