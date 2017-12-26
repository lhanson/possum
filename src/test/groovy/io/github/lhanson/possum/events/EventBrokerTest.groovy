package io.github.lhanson.possum.events

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.scene.Scene
import io.github.lhanson.possum.system.GameSystem
import spock.lang.Specification

class EventBrokerTest extends Specification {
	EventBroker broker
	GameEntity subscriber

	def setup() {
		broker = new EventBroker()
		subscriber = new GameEntity() {
			@Subscription
			void notify(String event) { }
		}
	}

	def "Annotated subscribers can register event handlers with the broker"() {
		when:
			broker.subscribe(subscriber)

		then:
			broker.subscriptionsByEventClass[String][0].subscriber == subscriber
	}

	def "Events without subscribers are fine"() {
		when:
			int notifications = broker.publish('String event')

		then:
			notifications == 0
	}

	def "Registering a subscriber with no annotations does nothing"() {
		when:
			broker.subscribe(new GameEntity())

		then:
			broker.subscriptionsByEventClass.isEmpty()
	}

	def "Event reaches subscriber"() {
		given:
			broker.subscribe(subscriber)

		when:
			int notified = broker.publish('String event')

		then:
			notified == 1
	}

	def "Event reaches multiple subscribers"() {
		given:
			broker.subscribe(subscriber)
			broker.subscribe(new GameSystem() {
				String name = 'Test System'
				@Override void doUpdate(Scene scene, double elapsed) { }
				@Subscription void notify(String event) { }
			})
			def event = 'String event'

		when:
			int notified = broker.publish(event)

		then:
			notified == 2
	}

	def "Non-annotated subscription"() {
		given:
			boolean handled = false
			def handler = { handled = true }
			broker.subscribe(this, SceneInitializedEvent, handler)

		when:
			broker.publish(SceneInitializedEvent)

		then:
			handled
	}

}
