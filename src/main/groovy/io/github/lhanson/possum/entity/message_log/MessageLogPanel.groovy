package io.github.lhanson.possum.entity.message_log

import io.github.lhanson.possum.component.AreaComponent
import io.github.lhanson.possum.component.GameComponent
import io.github.lhanson.possum.component.TextComponent
import io.github.lhanson.possum.entity.PanelEntity

/**
 * Panel used for displaying an ongoing series of messages
 */
class MessageLogPanel extends PanelEntity {
	// Tracks the last message added to the panel
	private RepeatableMessage previousMessage

	/**
	 * Adds the given text to the panel, using RepeatableMessage functionality
	 * to avoid duplicating the same text multiple times.
	 *
	 * @param message the text to add to the panel
	 */
	void addMessage(String message) {
		if (previousMessage?.baseText == message) {
			println "Incrementing message"
			previousMessage.repeat()
			// TODO: what about previous location?
			scene?.entityNeedsRendering(previousMessage)
		} else {
			previousMessage = new RepeatableMessage(message)
			println "Adding $previousMessage..."
			previousMessage.parent = this
			inventory.add(previousMessage)
			if (inventory.size() > area.height - padding.height) {
				log.debug "Scrolling $name up"
				scrollUp()
			}
		}
//		println "Added, computing inventory positions"
//		computeInventoryPositions() // TODO: when does this happen?
//		computeArea()
		// TODO: add previous area of last line to blank out longer text
		// TODO: probably need to re-render every visible inventory item of the panel...
	}

	/** Moves all inventory items up one increment */
	void scrollUp() {
		// TODO: maybe do pairwise scroll so we can render each as we go?
		inventory.each {
			it.getComponentOfType(AreaComponent).y--
		}
	}

}
