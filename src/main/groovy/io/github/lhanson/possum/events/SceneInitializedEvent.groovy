package io.github.lhanson.possum.events

import groovy.transform.ToString

/**
 * Event signaling the completion of a scene's initialization
 */
@ToString
class SceneInitializedEvent {
	String sceneId
	SceneInitializedEvent(String sceneId) {
		this.sceneId = sceneId
	}
}
