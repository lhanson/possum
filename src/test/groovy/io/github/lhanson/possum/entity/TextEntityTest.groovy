package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.TextComponent
import spock.lang.Specification

class TextEntityTest extends Specification {
	String text = 'test text'

	def "Shorthand constructor for setting TextComponent"() {
		when:
			TextEntity te = new TextEntity(text)
		then:
			te.getComponentOfType(TextComponent).text == text
	}

	def "Text entity is guaranteed to come with a TextComponent and an AreaComponent"() {
		when:
			TextEntity te = new TextEntity()
		then:
			te.getComponentOfType(TextComponent)
			te.getComponentOfType(AreaComponent)
	}

	def "Calculate basic text area"() {
		when:
			TextEntity te = new TextEntity(text)
			AreaComponent area = te.getComponentOfType(AreaComponent)
		then:
			area.width == 'test text'.size()
			area.height == 1
	}

	def "Text entity without explicit area will compute it as needed"() {
		when:
			TextEntity te = new TextEntity(text)
			AreaComponent area = te.getComponentOfType(AreaComponent)
		then:
			area.width == text.length()
			area.height == 1
	}

	def "Getting text will create a TextComponent when necessary"() {
		when:
			TextEntity te = new TextEntity()
		then:
			te.text == ''
	}

	def "Setting text will create a TextComponent when necessary"() {
		given:
			TextEntity te = new TextEntity()
		when:
			te.text = 'test text'
		then:
			te.getComponentOfType(TextComponent).text == text
	}

}
