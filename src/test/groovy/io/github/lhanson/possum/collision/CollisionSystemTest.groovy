package io.github.lhanson.possum.collision

import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.events.CollisionEvent
import io.github.lhanson.possum.events.EventBroker
import spock.lang.Specification

class CollisionSystemTest extends Specification {

	def "Collision handling causes an event to be published"() {
		given:
			CollisionEvent collisionEvent
			EventBroker eventBroker = new EventBroker()
			eventBroker.subscribe(this, CollisionEvent, { collisionEvent = it })
			CollisionSystem collisionSystem = new CollisionSystem(eventBroker: eventBroker)

		when:
			collisionSystem.collide(new GameEntity(name: 'entity'), new GameEntity(name: 'collider'))

		then:
			collisionEvent
	}

}
