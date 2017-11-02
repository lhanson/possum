package io.github.lhanson.possum.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

/**
 * Synchronous event notification broker. Subscribers can
 * register to be notified of particular event types and
 * have a corresponding {@link Subscription}-annotated
 * method called whene such events are published.
 */
class EventBroker {
	Logger log = new LoggerFactory().getLogger(this.class)
	Map<Class, List<Subscription>> subscriptionsByEventClass = [:]

	/**
	 * Register any Subscription-annotated methods with the broker
	 * @param subscriber a subscriber with annotated subscription methods
	 */
	void subscribe(Object subscriber) {
		// Find all methods that have @Subscription
		subscriber.class.methods.findAll { Method method ->
			method.getAnnotation(io.github.lhanson.possum.events.Subscription) && method.parameterTypes.length == 1
		}.each { Method method ->
			Class eventClass = method.parameterTypes[0]
			List<Subscription> subscriptions = subscriptionsByEventClass[eventClass]
			if (subscriptions == null) {
				subscriptions = []
				subscriptionsByEventClass.put(eventClass, subscriptions)
			}
			subscriptions.add(new Subscription(subscriber, method))
			log.debug("Registered subscription for {} to event type {}", subscriber, eventClass)
		}
	}

	/**
	 * Invokes the {@code Subscription}-annotated method
	 * on each subscriber registered for this type of event.
	 * @param event the event to send to subscribers
	 * @return the number of notifications sent
	 */
	int publish(Object event) {
		log.debug("Publishing {}", event)
		List<Subscription> subscriptions = subscriptionsByEventClass[event.class] ?: []
		return subscriptions.each {
			log.debug("Notifying {} of event {}", it.subscriber, event)
			it.notify(event)
		}.size()
	}

	/**
	 * Record of a subscribing object and its callback method.
	 */
	static class Subscription {
		Object subscriber
		Method method

		Subscription(Object subscriber, Method method) {
			this.subscriber = subscriber
			this.method = method
		}

		void notify(Object event) {
			method.invoke(subscriber, event)
		}
	}

}
