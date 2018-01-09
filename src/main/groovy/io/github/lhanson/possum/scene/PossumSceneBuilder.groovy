package io.github.lhanson.possum.scene

import io.github.lhanson.possum.events.EventBroker
import io.github.lhanson.possum.events.SceneInitializedEvent
import io.github.lhanson.possum.input.InputAdapter
import io.github.lhanson.possum.rendering.RenderingSystem
import io.github.lhanson.possum.system.GameSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class PossumSceneBuilder {
	static String START = 'start' // The scene ID every game starts in by default
	static String PREVIOUS = 'pop_previous_scene' // The scene ID indicating that the previous scene should be restored
	Logger log = LoggerFactory.getLogger(this.class)
	String nextSceneId = START
	Scene currentScene
	Map<String, Scene> scenesById = [:]
	Stack<Scene> sceneStack = new Stack()
	boolean pushScene
	@Autowired List<RenderingSystem> renderers
	@Autowired(required = false) List<GameSystem> systems
	@Autowired EventBroker eventBroker
	@Autowired Random rand

	@Autowired
	InputAdapter inputAdapter

	/**
	 * Register a new {@code Scene} with the game
	 * @param scene the scene to register
	 */
	void addScene(Scene scene) {
		// We do a bit of manual wiring of dependencies
		// here since Scene isn't a Spring Bean.
		scene.inputAdapter = inputAdapter
		scene.eventBroker = eventBroker
		if (scene.loadingScene) {
			scene.loadingScene.eventBroker = eventBroker
		}
		scenesById[scene.id] = scene
		if (scene.loadingScene) {
			addScene(scene.loadingScene)
		}
	}

	/**
	 * Return the active {@link Scene}.
	 *
	 * Components within a Scene will maintain state and determine when
	 * to transition to a different scene, which is indicated by setting
	 * {@code nextScene} to the ID of the next scene to be run.
	 *
	 * @return the Scene corresponding to the {@code nextSceneId}
	 */
	Scene getNextScene() {
		if (nextSceneId == PREVIOUS) {
			nextSceneId = sceneStack.pop()?.id
			log.info "Popped previous scene '$nextSceneId' off the stack"
		}
		// This scene will loop until any of its components specify otherwise
		Scene nextScene = scenesById[nextSceneId]
		if (nextScene && nextScene != currentScene) {
			log.info "Scene change detected from {} to {}", currentScene?.id, nextScene?.id
			// If the next scene isn't initialized yet but has a loading scene, run the loading scene
			if (!nextScene.initialized && nextScene.loadingScene && !nextScene.loadingScene.initialized) {
				initLoadingScene(nextScene)
				nextScene = nextScene.loadingScene
			}
			if (pushScene) {
				sceneStack.push(currentScene)
			} else if (currentScene) {
				currentScene.uninit()
				systems.each { it.uninitScene(currentScene) }
				renderers.each { it.uninitScene(currentScene) }
			}
			if (!nextScene.initialized) {
				nextScene.init()
			}
			systems.each { it.initScene(nextScene) }
			renderers.each { it.initScene(nextScene) }
			currentScene = nextScene
		}
		return nextScene
	}

	void initLoadingScene(Scene nextScene) {
		log.info "Running loading scene {}, while waiting for {} to initialize", nextScene.loadingScene, nextScene.id
		def handler = { String nextId, SceneInitializedEvent event ->
			if (event.sceneId == nextId) {
				transition(nextId)
			}
		}.curry(nextScene.id)

		eventBroker.subscribe(this, SceneInitializedEvent, handler)
		final Scene backgroundLoadedScene = nextScene
		Thread.start { backgroundLoadedScene.init() }
		transition(nextScene.loadingScene.id)
	}

	/**
	 * Indicates that the next iteration of the main loop should run
	 * the specified scene.
	 *
	 * @param nextSceneId the scene to run next, or null if the program should exit
	 * @param push whether to pause the current scene and push it onto the active stack;
	 *             if not, the current scene will be uninitialized
	 */
	def transition = { String nextSceneId, boolean push = false ->
		this.nextSceneId = nextSceneId
		this.pushScene = push
	}

}
