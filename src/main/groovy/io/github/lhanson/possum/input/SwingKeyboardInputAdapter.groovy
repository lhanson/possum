package io.github.lhanson.possum.input

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.swing.*
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.List
import java.util.concurrent.ConcurrentLinkedQueue

@Component
class SwingKeyboardInputAdapter implements InputAdapter, KeyListener {
	private Logger log = LoggerFactory.getLogger(this.class)
	private ConcurrentLinkedQueue<KeyEvent> queuedKeyEvents = new ConcurrentLinkedQueue<>()

	@Autowired
	SwingKeyboardInputAdapter(JFrame jframe) {
		log.debug "Adding key listener to jFrame"
		jframe.addKeyListener(this)
	}

	@Override
	List<RawInput> collectInput() {
		if (queuedKeyEvents) {
			log.trace "Processing queued input"
		}
		def keyInput = []
		queuedKeyEvents.each {
			log.trace "Key event ${it.keyChar} (${it.keyCode}): ${it.ID}"
			if (it.ID == Event.KEY_PRESS) {
				switch (it.keyCode) {
					case KeyEvent.VK_ESCAPE:
						keyInput << RawInput.ESCAPE
						break
					case KeyEvent.VK_ENTER:
						keyInput << RawInput.ENTER
						break
					case KeyEvent.VK_LEFT:
						keyInput << RawInput.LEFT
						break
					case KeyEvent.VK_RIGHT:
						keyInput << RawInput.RIGHT
						break
					case KeyEvent.VK_UP:
						keyInput << RawInput.UP
						break
					case KeyEvent.VK_DOWN:
						keyInput << RawInput.DOWN
						break
				}
			}
		}
		queuedKeyEvents.clear()
		return keyInput
	}

	@Override void keyTyped(KeyEvent e) { }

	@Override
	void keyPressed(KeyEvent e) {
		log.trace "Key pressed: ${e.keyChar} (${e.keyCode})"
		queuedKeyEvents << e
	}

	@Override
	void keyReleased(KeyEvent e) { }
}
