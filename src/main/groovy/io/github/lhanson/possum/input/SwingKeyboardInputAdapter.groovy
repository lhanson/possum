package io.github.lhanson.possum.input

import io.github.lhanson.possum.scene.PossumSceneBuilder
import io.github.lhanson.possum.system.PauseSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.swing.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.concurrent.ConcurrentLinkedQueue

@Component
class SwingKeyboardInputAdapter implements InputAdapter, KeyListener {
	private Logger log = LoggerFactory.getLogger(this.class)
	private ConcurrentLinkedQueue<KeyEvent> queuedKeyEvents = new ConcurrentLinkedQueue<>()

	@Autowired PauseSystem pauseSystem
	@Autowired PossumSceneBuilder sceneBuilder

	@Autowired
	SwingKeyboardInputAdapter(JFrame jframe) {
		log.debug "Adding key listener to jFrame"
		jframe.addKeyListener(this)
	}

	@Override
	List<InputEvent> collectInput() {
		def collectedInput = queuedKeyEvents.collect {it}
		queuedKeyEvents.clear()
		return collectedInput
	}

	@Override void keyTyped(KeyEvent e) { }

	@Override
	void keyPressed(KeyEvent e) {
		log.trace "Key pressed: ${e.keyChar} (${e.keyCode})"
		String sceneId = sceneBuilder.currentScene.id
		if (!pauseSystem.paused(sceneId)) {
			queuedKeyEvents << e
		} else if (e.keyChar == 'p') {
			synchronized(sceneBuilder.currentScene) {
				log.debug "Unpausing scene"
				pauseSystem.unpause(sceneId)
				pauseSystem.currentScene.notify()
			}
		}
	}

	@Override
	void keyReleased(KeyEvent e) { }
}
