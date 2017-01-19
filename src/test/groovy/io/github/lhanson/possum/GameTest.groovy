package io.github.lhanson.possum

import io.github.lhanson.possum.system.GameSystem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest
class GameTest extends Specification {
	@Autowired GameSystem gameState

	def "Context loads with a GameState system"() {
		when:
			true
		then:
			gameState
	}
}