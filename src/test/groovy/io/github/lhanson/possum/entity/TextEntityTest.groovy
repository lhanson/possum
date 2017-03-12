package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.TextComponent
import spock.lang.Specification

class TextEntityTest extends Specification {

	def "Calculate basic text area"() {
		given:
			String text = 'test text'
			TextEntity te = new TextEntity(
					components: [new TextComponent(text)])

		when:
			def area = te.calculateArea()

		then:
			area.width == 'test text'.size()
			area.height == 1
	}

}
