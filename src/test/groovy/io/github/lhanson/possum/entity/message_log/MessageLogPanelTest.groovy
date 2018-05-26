package io.github.lhanson.possum.entity.message_log

import io.github.lhanson.possum.component.AreaComponent
import spock.lang.Specification

class MessageLogPanelTest extends Specification {
	MessageLogPanel messageLog

	def setup() {
		messageLog = new MessageLogPanel()
	}

	def "Adding a single text entry"() {
		when:
			messageLog.addMessage('Hello')
			def inventory = messageLog.inventory
		then:
			inventory.size() == 1
			inventory[0].text == 'Hello'
	}

	def "Adding multiple text components"() {
		when:
			messageLog.addMessage('Hello')
			messageLog.addMessage('Goodbye')
			def inventory = messageLog.inventory
		then:
			inventory.size() == 2
			inventory[0].text == 'Hello'
			inventory[1].text == 'Goodbye'
	}

	def "Repeatable messages reference the panel as their parent"() {
		when:
			messageLog.addMessage('Hello')
		then:
			messageLog.inventory[0].parent == messageLog
	}

	def "Message log only scrolls once inventory exceeds available space"() {
		given:
			int scrollCount = 0
			messageLog = new MessageLogPanel() {
				@Override void scrollUp() {
					scrollCount++
				}
			}
		when: 'The first message is added'
			messageLog.addMessage('Hello')
		then:
			scrollCount == 0

		when: 'A second message is added'
			messageLog.addMessage('Goodbye')
		then: 'Scroll is called once'
			scrollCount == 1
	}

	def "Scrolling up moves all inventory items up one space"() {
		given:
			def helloY = { messageLog.inventory[0].getComponentOfType(AreaComponent).y }

		when: 'First message is added'
			messageLog.addMessage('Hello')
		then: 'it sits at y == 0'
			helloY() == 0

		when: 'Second message is added and a scroll is required'
			messageLog.addMessage('Goodbye')
		then: 'the first message gets scrolled up'
			helloY() == -1
	}

}
