package io.github.lhanson.possum.events

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.scene.Scene
import io.github.lhanson.possum.system.GameSystem
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

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

	def "Unsubscribe"() {
		when:
			broker.subscribe(subscriber)
			broker.unsubscribe(subscriber)

		then:
			broker.subscriptionsByEventClass[String] == []
	}

	/*
	 * As much as one tries to make tests self-documenting, this multithreaded business might merit some
	 * narrative. The potential problem this reproduces is one thread iterating over a list of subscriptions
	 * in `subscriptionsByEventClass` while another thread is unsubscribing from subscriptions; modification
	 * of the collection while it is being iterated throws a ConcurrentModificationException. Our implementation
	 * makes a copy of the list before iterating it.
	 *
	 * The jumble of locking isn't pretty, but it lets us start a background thread and block it until
	 * we've begun iterating the list; then it blocks us while it modifies the list, and finally unblocks
	 * our iteration (at which point a concurrent modification would raise an exception).
	 * The alternative of arbitrary sleep() between the threads is both nondeterministic and unnecessarily slow.
	 */
	def "Modification of subscription list is threadsafe even when iterating to publish"() {
		given: 'A list of subscribers'
			List<GameEntity> subscribers = []
			AtomicInteger handledCount = new AtomicInteger()
			ReentrantLock unsubscribeLock = new ReentrantLock()
			ReentrantLock continueLock = new ReentrantLock()
			unsubscribeLock.lock() // block unsubscriber until we've started iterating subscriber list

			(0..9).each { int subscriberNum ->
				def testSubscriber = new GameEntity() { @Subscription void _(def _) {} }
				if (subscriberNum == 2) {
					// Insert one handler as a 'poison pill' to coordinate the background modification
					broker.subscribe(testSubscriber, SceneInitializedEvent, {
						unsubscribeLock.unlock() // let the other thread do an unsubscription
						continueLock.lock()      // wait until it's done unsubscribing
						handledCount.incrementAndGet()
					})
				} else {
					broker.subscribe(testSubscriber, SceneInitializedEvent, { handledCount.incrementAndGet() })
				}
				subscribers << testSubscriber
			}

		when: 'Wait for the notifications to start being published and then yank one off the end of the collection'
			Thread.start {
				continueLock.lock()    // block publisher thread until we've tampered with the list
				unsubscribeLock.lock() // wait until publisher starts notifying subscribers
				broker.unsubscribe(subscribers.last()) // modify subscription list while publisher is blocked
				continueLock.unlock() // unblock publisher to continue notifying with altered backing list
			}
			broker.publish(SceneInitializedEvent)

		then: 'All subscriptions present at the start were notified'
			handledCount.get() == 10
	}

}
