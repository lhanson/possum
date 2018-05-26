package io.github.lhanson.possum.entity.message_log

import io.github.lhanson.possum.component.AreaComponent
import spock.lang.Specification

class RepeatableMessageTest extends Specification {

	def "Base text is tracked"() {
		when:
			RepeatableMessage message = new RepeatableMessage('foo')
		then:
			message.baseText == 'foo'
	}

	def "Area calculation is updated after repeating"() {
		given:
			RepeatableMessage message = new RepeatableMessage('foo')
			int initialWidth = message.getComponentOfType(AreaComponent).width
		when:
			message.repeat()
		then:
			message.text == 'foo (x2)'
			message.getComponentOfType(AreaComponent).width != initialWidth
	}

}
