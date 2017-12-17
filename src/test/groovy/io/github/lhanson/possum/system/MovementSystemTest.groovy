package io.github.lhanson.possum.system

import io.github.lhanson.possum.collision.CollisionSystem
import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.scene.Scene
import spock.lang.Specification

class MovementSystemTest extends Specification {
	MovementSystem movementSystem

	def setup() {
		movementSystem = new MovementSystem(
				collisionSystem: Mock(CollisionSystem),
				movingEntities: [])
	}

	def "Simple bounding box is correctly computed"() {
		given:
			def areas = [
					new AreaComponent(0, 0, 1, 1),
					new AreaComponent(1, 1, 1, 1)
			]
		when:
			AreaComponent boundingBox = movementSystem.boundingBox(areas)
		then:
			boundingBox.x == 0
			boundingBox.y == 0
			boundingBox.width == 1
			boundingBox.height == 1
	}

	def "More complex bounding box is correctly computed"() {
		given:
			def areas = [
					new AreaComponent(100, 10, 10, 10),
					new AreaComponent(101, 15, 10, 10),
					new AreaComponent(200, 100, 10, 10),
					new AreaComponent(130, 500, 10, 10),
					new AreaComponent(100, 10, 10, 10),
			]
		when:
			AreaComponent boundingBox = movementSystem.boundingBox(areas)
		then:
			boundingBox.x == 100
			boundingBox.y == 10
			boundingBox.width == 100
			boundingBox.height == 490
	}

	def "Duplicate input signals are not handled each frame"() {
		given:
			GameEntity hero = heroAt(0, 0)
			Scene scene = new Scene('testScene', [hero], null)
			scene.activeInput.addAll([MappedInput.RIGHT, MappedInput.RIGHT])
		when:
			movementSystem.update(scene, 0)
		then:
			// Having two queued right inputs shouldn't move twice
			hero.getComponentOfType(AreaComponent) == new AreaComponent(1, 0, 1, 1)
	}

	GameEntity heroAt(int x, int y) {
		new GameEntity(
				name: 'hero',
				components: [
						new TextComponent('@'),
						new AreaComponent(x, y, 1, 1),
						new VelocityComponent(0, 0),
						new PlayerInputAwareComponent()
				])
	}

}
