package io.github.lhanson.possum.entity.menu

import io.github.lhanson.possum.input.MappedInput
import spock.lang.Specification

class IntegerItemEntityTest extends Specification {

	def "Integer item entities enforce min/max bounds on creation"() {
		when:
			new IntegerItemEntity('item', 100, 0, 10)
		then:
			thrown IllegalArgumentException

		when:
			new IntegerItemEntity('item', -100, 0, 10)
		then:
			thrown IllegalArgumentException
	}

	def "Integer item setter handles lack of min/max bounds gracefully"() {
		given:
			IntegerItemEntity item = new IntegerItemEntity('item', 1)
		when:
			item.setValue(2)
		then:
			item.value == 2
	}

	def "Integer item setter handles min/max bounds gracefully"() {
		when:
			IntegerItemEntity item = new IntegerItemEntity('item', 5, 0, 10)
		then:
			!item.setValue(item.max + 1)
			item.value == 5
	}

	def "Integer item increment methods handle min/max bounds gracefully"() {
		given:
			IntegerItemEntity item = new IntegerItemEntity('item', 0, 0, 0)
		when:
			def updatedEntities = item.handleInput(MappedInput.RIGHT)
		then:
			!updatedEntities
			item.value == 0

		when:
			updatedEntities = item.handleInput(MappedInput.LEFT)
		then:
			!updatedEntities
			item.value == 0
	}

}

