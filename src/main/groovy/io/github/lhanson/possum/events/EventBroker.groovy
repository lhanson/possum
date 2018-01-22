package io.github.lhanson.possum.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import java.lang.reflect.Method

/**
 * Synchronous event notification broker. Subscribers can
 * register to be notified of particular event types and
 * have a corresponding {@link Subscription}-annotated
 * method called when such events are published.
 */
@Component
class EventBroker {
	Logger log = new LoggerFactory().getLogger(this.class)
	Map<Class, List<Subscription>> subscriptionsByEventClass = [:]

	/**
	 * Register any Subscription-annotated methods on subscriber with the broker
	 *
	 * @param subscriber a subscriber with annotated subscription methods
	 */
	void subscribe(Object subscriber) {
		// Find all methods that have @Subscription
		subscriber.class.methods.findAll { Method method ->
			method.getAnnotation(io.github.lhanson.possum.events.Subscription) && method.parameterTypes.length == 1
		}.each { Method method ->
			subscribe(subscriber, method.parameterTypes[0], method)
		}
	}

	/**
	 * Subscribe the provided object to the specified event type.
	 * Invoked by the annotation-scanning version above, or directly
	 * by handlers implemented with closures rather than annotated methods.
	 *
	 * @param subscriber a subscriber with annotated subscription methods
	 * @param eventClass an optional parameter to specify which event the caller
	 *        wishes to subscribe to rather than being annotated
	 * @param handler the closure or method to execute when a matching event is fired
	*/
	void subscribe(Object subscriber, Class eventClass, def handler) {
		List<Subscription> subscriptions = subscriptionsByEventClass[eventClass]
		if (subscriptions == null) {
			subscriptions = []
			subscriptionsByEventClass.put(eventClass, subscriptions)
		}
		subscriptions.add(new Subscription(subscriber, handler))
		log.debug("Registered subscription for {} to event type {}", subscriber, eventClass)
	}

	/** Remove any subscriptions registered by the provided subscriber */
	void unsubscribe(Object subscriber) {
		subscriptionsByEventClass.values().each { List<Subscription> subscriptions ->
			log.debug "Unsubscribing {} from {}", subscriber, subscriptions
			subscriptions.removeAll { it.subscriber == subscriber }
		}
	}

	/**
	 * Invokes the {@code Subscription}-annotated method
	 * on each subscriber registered for this type of event.
	 *
	 * @param event the event to send to subscribers
	 * @return the number of notifications sent
	 */
	int publish(Object event) {
		log.debug("Publishing {}", event)
		int published = 0
		def key = (event instanceof Class) ? event : event.class
		List<Subscription> subscriptions = subscriptionsByEventClass[key]
		if (subscriptions) {
			// Use collect() to work on a copy of the list to avoid ConcurrentModificationExceptions
			// when unsubscriptions while we're iterating.
			subscriptions.collect().each {
				log.debug("Notifying {} of event {}", it.subscriber, event)
				it.notify(event)
				published++
			}
		}
		return published
	}

	/**
	 * Record of a subscribing object and its callback method.
	 */
	static class Subscription {
		Object subscriber
		def handler

		Subscription(Object subscriber, def handler) {
			this.subscriber = subscriber
			this.handler = handler
		}

		void notify(Object event) {
			if (handler instanceof Method) {
				handler.invoke(subscriber, event)
			} else if (handler instanceof Closure) {
				handler.call(event)
			} else {
				throw new IllegalStateException("Unknown event handler type (${handler.class}) for subscriber $subscriber")
			}
		}

		@Override
		String toString() { "subscriber is a ${subscriber.class}"}
	}

}
