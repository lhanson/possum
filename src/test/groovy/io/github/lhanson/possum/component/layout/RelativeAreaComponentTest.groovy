package io.github.lhanson.possum.component.layout

import spock.lang.Specification

class RelativeAreaComponentTest extends Specification {
	RelativeAreaComponent relativeAreaComponent

	def setup() {
		relativeAreaComponent = new RelativeAreaComponent()
	}

	def "When width is set, relative width cannot be set"() {
		given:
			relativeAreaComponent.width = 1
		when:
			relativeAreaComponent.relativeWidth = 1
		then:
			thrown IllegalStateException
	}

	def "When relative width is set, width cannot be set"() {
		given:
			relativeAreaComponent.relativeWidth = 1
		when:
			relativeAreaComponent.width = 1
		then:
			thrown IllegalStateException
	}

	def "Relative width is interpreted as a percentage and must be between 0 and 100, inclusive"() {
		when:
			relativeAreaComponent.relativeWidth = -1
		then:
			thrown IllegalArgumentException

		when:
			relativeAreaComponent.relativeWidth = 101
		then:
			thrown IllegalArgumentException
	}

	def "When height is set, relative height cannot be set"() {
		given:
			relativeAreaComponent.height = 1
		when:
			relativeAreaComponent.relativeHeight = 1
		then:
			thrown IllegalStateException
	}

	def "When relative height is set, height cannot be set"() {
		given:
			relativeAreaComponent.relativeHeight = 1
		when:
			relativeAreaComponent.height = 1
		then:
			thrown IllegalStateException
	}

	def "Relative height is interpreted as a percentage and must be between 0 and 100, inclusive"() {
		when:
			relativeAreaComponent.relativeHeight = -1
		then:
			thrown IllegalArgumentException

		when:
			relativeAreaComponent.relativeHeight = 101
		then:
			thrown IllegalArgumentException
	}
}
