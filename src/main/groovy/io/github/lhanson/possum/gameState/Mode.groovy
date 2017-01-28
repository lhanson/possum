package io.github.lhanson.possum.gameState

/**
 * High-level descriptions of game modes. These can be
 * used to transition between sets of game entities which
 * should be active at any given time.
 */
enum Mode {
	MAIN_MENU,
	PLAYING,
	QUITTING,
	EXIT
}