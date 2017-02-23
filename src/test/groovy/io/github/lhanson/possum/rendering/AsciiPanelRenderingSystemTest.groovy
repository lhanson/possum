package io.github.lhanson.possum.rendering

import asciiPanel.AsciiPanel
import io.github.lhanson.spring.TestApplicationContextLoader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification


@ContextConfiguration(classes = AsciiPanelRenderingSystem, loader = TestApplicationContextLoader)
class AsciiPanelRenderingSystemTest extends Specification {
	@Autowired AsciiPanelRenderingSystem renderer

	def "Write ignores objects outside the viewport"() {
		given:
			renderer.terminal = Mock(AsciiPanel)
		when:
			renderer.write('s', -1, -1)
			renderer.write('s', renderer.viewport.width, renderer.viewport.height)
		then:
			0 * _
	}

}
