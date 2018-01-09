package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.TextComponent
import spock.lang.Specification

class TextEntityTest extends Specification {
	String text = 'test text'

	def "Text entity is guaranteed to come with a TextComponent and an AreaComponent"() {
		given:
			TextEntity te = new TextEntity()
		when:
			te.init()
		then:
			te.getComponentOfType(TextComponent)
			te.getComponentOfType(AreaComponent)
	}

	def "Calculate basic text area"() {
		given:
			TextEntity te = new TextEntity(components: [new TextComponent(text)])
			te.init()
		when:
			AreaComponent area = te.getComponentOfType(AreaComponent)
		then:
			area.width == 'test text'.size()
			area.height == 1
	}

	def "Text entity without explicit area will compute it as needed"() {
		given:
			String text = 'test text'
			TextEntity te = new TextEntity(components: [new TextComponent(text)])
			te.init()
		when:
			AreaComponent area = te.getComponentOfType(AreaComponent)
		then:
			area.width == text.length()
			area.height == 1
	}

}
