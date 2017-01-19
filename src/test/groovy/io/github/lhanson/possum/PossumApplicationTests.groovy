package io.github.lhanson.possum

import io.github.lhanson.possum.system.GameSystem
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner)
@SpringBootTest
class PossumApplicationTests {
	@Autowired GameSystem gameState

	@Test
	void contextLoads() {
	}

	@Test
	void hasGameState() {
		assert gameState != null
	}
}
