package io.github.lhanson.possum.system

import io.github.lhanson.possum.component.AnimatedComponent
import io.github.lhanson.possum.entity.GameEntity
import io.github.lhanson.possum.scene.Scene
import spock.lang.Specification

class AnimationSystemTest extends Specification {
	AnimationSystem animationSystem

	def setup() {
		animationSystem = new AnimationSystem()
	}

	def "Animation durations are updated properly"() {
		given:
			AnimatedComponent animatedComponent = new AnimatedComponent(pulseDurationMillis: 100)
			Scene scene = new Scene('scene',
					[new GameEntity(components: [animatedComponent])],
					[])
		when:
			animationSystem.doUpdate(scene, 10)
		then:
			println "Entity: ${scene.entities[0]}"
			scene.entities[0].components[0] == animatedComponent
			animatedComponent.currentDuration == 10
	}

	def "Animated components are removed after completion"() {
		given:
			AnimatedComponent animatedComponent = new AnimatedComponent(pulseDurationMillis: 100)
			Scene scene = new Scene('scene',
					[new GameEntity(components: [animatedComponent])],
					[])
		when:
			animationSystem.doUpdate(scene, 100)
		then:
			scene.entities[0].getComponentsOfType(AnimatedComponent).isEmpty()
	}

	def "getPulsedAlpha opaque lower bound"() {
		given:
			AnimatedComponent ac = new AnimatedComponent(currentDuration: 0, pulseDurationMillis: 100)

		when:
			int alpha = animationSystem.getPulsedAlpha(ac)

		then:
			alpha == 255
	}

	def "getPulsedAlpha opaque upper bound"() {
		given:
			AnimatedComponent ac = new AnimatedComponent(currentDuration: 100, pulseDurationMillis: 100)

		when:
			int alpha = animationSystem.getPulsedAlpha(ac)

		then:
			alpha == 255
	}

	def "getPulsedAlpha transparent middle region"() {
		given:
			AnimatedComponent ac = new AnimatedComponent(currentDuration: 50, pulseDurationMillis: 100)

		when:
			int alpha = animationSystem.getPulsedAlpha(ac)

		then:
			alpha == 127
	}

}