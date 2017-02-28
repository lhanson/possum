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
		movementSystem = new MovementSystem(collisionSystem: Mock(CollisionSystem))
	}

	def "Simple bounding box is correctly computed"() {
		given:
			def positions = [
					new PositionComponent(0, 0),
					new PositionComponent(1, 1)
			]
		when:
			def boundingBox = movementSystem.boundingBox(positions)
		then:
			boundingBox.size() == 2
			boundingBox[0] == new PositionComponent(0, 0)
			boundingBox[1] == new PositionComponent(1, 1)
			boundingBox[1] != new PositionComponent(10, 10)
	}

	def "More complex bounding box is correctly computed"() {
		given:
			def positions = [
					new PositionComponent(100, 10),
					new PositionComponent(101, 15),
					new PositionComponent(200, 100),
					new PositionComponent(130, 500),
					new PositionComponent(100, 10),
			]
		when:
			def boundingBox = movementSystem.boundingBox(positions)
		then:
			boundingBox.size() == 2
			boundingBox[0] == new PositionComponent(100, 10)
			boundingBox[1] == new PositionComponent(200, 500)
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
			hero.getComponentOfType(PositionComponent) == new PositionComponent(1, 0)
	}

	def "findAt works with no matches"() {
		given:
			GameEntity testEntity = new GameEntity(name: 'testEntity')
		when:
			List<GameEntity> result = movementSystem.findAt([testEntity], new PositionComponent(0, 0))
		then:
			result == []
	}

	GameEntity heroAt(int x, int y) {
		new GameEntity(
				name: 'hero',
				components: [
						new TextComponent('@'),
						new PositionComponent(x, y),
						new VelocityComponent(0, 0),
						new PlayerInputAwareComponent()
				])
	}

}
