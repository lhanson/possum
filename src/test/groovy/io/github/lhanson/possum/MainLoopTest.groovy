package io.github.lhanson.possum

import io.github.lhanson.possum.gameState.GameState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest
class MainLoopTest extends Specification {
	@Autowired GameState gameState

	def "Context loads with a GameState"() {
		when:
			true
		then:
			gameState
	}
}