package io.github.lhanson.possum.system

import io.github.lhanson.possum.collision.CollisionSystem
import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.input.MappedInput
import io.github.lhanson.possum.scene.Scene
import io.github.lhanson.possum.scene.SceneBuilder
import spock.lang.Specification

class MovementSystemTest extends Specification {
	MovementSystem movementSystem
	Scene scene

	def setup() {
		movementSystem = new MovementSystem(
				collisionSystem: Mock(CollisionSystem),
				eventBroker: new EventBroker())
		scene = SceneBuilder.createScene()
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
			scene.addEntity(hero)
			scene.activeInput.addAll([MappedInput.RIGHT, MappedInput.RIGHT])
		when:
			movementSystem.update(scene, 0)
		then:
			// Having two queued right inputs shouldn't move twice
			hero.getComponentOfType(AreaComponent) == new AreaComponent(1, 0, 1, 1)
	}

	def "Still entity has zero velocity"() {
		given:
			GameEntity hero = heroAt(0, 0)
			scene.addEntity(hero)
		when:
			movementSystem.update(scene, 0)
			VelocityComponent heroVelocity = hero.getComponentOfType(VelocityComponent)
		then:
			heroVelocity == movementSystem.still
	}

	def "Moving entities are stopped at the end of each frame"() {
		given:
			GameEntity hero = heroAt(0, 0)
			scene.addEntity(hero)
			scene.activeInput.addAll([MappedInput.RIGHT])
		when:
			movementSystem.update(scene, 0)
			VelocityComponent heroVelocity = hero.getComponentOfType(VelocityComponent)
		then:
			heroVelocity == movementSystem.still
			movementSystem.movingEntities[scene.id].empty
	}

	def "Moving entities are stored by scene"() {
		given:
			scene.addEntity(heroAt(0, 0, 1, 1))
			Scene scene2 = SceneBuilder.createScene({[heroAt(0, 0, 1, 1)]})
		when:
			movementSystem.initScene(scene)
			movementSystem.initScene(scene2)
		then:
			movementSystem.movingEntities[scene.id].size() == 1
			movementSystem.movingEntities[scene2.id].size() == 1
	}

	GameEntity heroAt(int x, int y, int dx = 0, int dy = 0) {
		new GameEntity(
				name: 'hero',
				components: [
						new TextComponent('@'),
						new AreaComponent(x, y, 1, 1),
						new VelocityComponent(dx, dy),
						new PlayerInputAwareComponent()
				])
	}

}
