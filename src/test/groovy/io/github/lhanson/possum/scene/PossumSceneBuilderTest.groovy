package io.github.lhanson.possum.scene

import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.input.InputAdapter
import spock.lang.Specification

class PossumSceneBuilderTest extends Specification {
	PossumSceneBuilder sceneBuilder

	def setup() {
		sceneBuilder = new PossumSceneBuilder(
				eventBroker: [:] as EventBroker,
				inputAdapter: [:] as InputAdapter
		)
	}

	def "Get next scene ID at start"() {
		when:
			String nextSceneId = sceneBuilder.nextSceneId
		then:
			nextSceneId == PossumSceneBuilder.START
	}

	def "Basic scene addition"() {
		given:
			Scene scene = new Scene('testScene', {})
		when:
			sceneBuilder.addScene(scene)
		then:
			sceneBuilder.scenesById['testScene'] == scene
	}

	def "Scene addition sets fields appropriately"() {
		given:
			InputAdapter inputAdapter = [:] as InputAdapter
			EventBroker eventBroker = [:] as EventBroker
			Scene scene = new Scene('testScene', {})

		when:
			sceneBuilder.inputAdapter = inputAdapter
			sceneBuilder.eventBroker = eventBroker
			sceneBuilder.addScene(scene)

		then:
			scene.inputAdapter == inputAdapter
			scene.eventBroker == eventBroker
	}

	def "Scene addition sets fields on loading scenes appropriately"() {
		given:
			EventBroker eventBroker = [:] as EventBroker
			Scene loadingScene = new Scene('loadingScene', {})
			Scene scene = new Scene('sceneWithLoading', {}, [], loadingScene)

		when:
			sceneBuilder.eventBroker = eventBroker
			sceneBuilder.addScene(scene)

		then:
			scene.loadingScene.eventBroker == eventBroker
	}

	def "Get next scene"() {
		given:
			Scene startScene = new Scene(PossumSceneBuilder.START, {})
		when:
			sceneBuilder.addScene(startScene)
			Scene nextScene = sceneBuilder.getNextScene()
		then:
			nextScene == startScene
	}

	def "Transition to next scene"() {
		given:
			Scene startScene = new Scene(PossumSceneBuilder.START, {})
			Scene scene2 = new Scene('scene2', {})
			sceneBuilder.addScene(startScene)
			sceneBuilder.addScene(scene2)

		when:
			sceneBuilder.transition(scene2.id)
			Scene nextScene = sceneBuilder.getNextScene()

		then:
			nextScene == scene2
	}

	def "Loading scenes are detected and added automatically"() {
		given:
			Scene loadingScene = new Scene('loading', {})
			Scene bigScene = new Scene(PossumSceneBuilder.START, {}, [], loadingScene)

		when:
			sceneBuilder.addScene(bigScene)

		then:
			sceneBuilder.scenesById['loading'] == loadingScene
	}

	def "Loading scenes are run while the parent scene is being initialized"() {
		given:
			Scene loadingScene = new Scene('loading', {})
			Scene bigScene = new Scene(PossumSceneBuilder.START, {}, [], loadingScene)

		when:
			sceneBuilder.addScene(bigScene)

		then:
			sceneBuilder.getNextScene() == loadingScene
			loadingScene.initialized
	}

}
