# Possum

[![Build Status](https://travis-ci.org/lhanson/possum.svg?branch=master)](https://travis-ci.org/lhanson/possum)
[![codecov](https://codecov.io/gh/lhanson/possum/branch/master/graph/badge.svg)](https://codecov.io/gh/lhanson/possum)
[![Dependency Status](https://www.versioneye.com/user/projects/584ea9225d8a550042585f1c/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/584ea9225d8a550042585f1c)
[![Dependency Status](https://dependencyci.com/github/lhanson/possum/badge)](https://dependencyci.com/github/lhanson/possum)

A trash-eating game engine.

![Let's eat trash and get hit by a car](https://s-media-cache-ak0.pinimg.com/736x/ca/20/41/ca20415ef281931b9bbf8abc7144d6ea.jpg)


# Overview

A JVM-based game engine written around the [Entity–component–system](https://en.wikipedia.org/wiki/Entity%E2%80%93component%E2%80%93system)
pattern. It's being initially developed for roguelikes, but since I can't restrict myself
to any particular UI or implementation technology, it's inherently flexible and agnostic
to your choice of rendering system or UI.


# Features

* Interfaces to support a variety of underlying I/O technologies.
  Out of the box, support for [AsciiPanel](https://github.com/trystan/AsciiPanel)
  rendering and Swing keyboard input adapter.
* Basic animation system using alpha blending
* Internal event broker
* Rendering debug mode with visual hints


# Writing a Possum-powered game

Create a Spring Boot project with classpath scanning enabled to pick up
beans from `io.github.lhanson.possum`, as well as your own packages.

    @SpringBootApplication(scanBasePackages = [
    		'io.github.lhanson.possum',
    		'[YOUR.PACKAGES.HERE]'
    ])
    class EatTrashGame {
    	static void main(String[] args) {
    		def context = new SpringApplicationBuilder(EatTrashGame)
    				.headless(false)
    				.web(false)
    				.run(args)
    		// Run the main game loop
    		context.getBean(MainLoop).run()
    	}
    }

That main loop won't do shit unless you implement a `PossumSceneBuilder` to
define your entities and input handling and whatever. You know, your game logic.
More on that junk to come.


# Engine Internals

## Main loop, time simulation, and rendering strategy

Possum strives to be performant and responsive on slow systems or with games containing a lot
of simulation complexity, while also being able to take good advantage of speedy hardware. To
that end, the main loop uses fixed simulation timesteps to update the game state, and
variable rendering frequency to adapt to hardware capabilities. Rather than flounder about,
I'll quote an explanation from Robert Nystrom's
[Game Programming Patterns](http://gameprogrammingpatterns.com/game-loop.html#play-catch-up):

    [This approach] updates with a fixed time step, but it can drop rendering frames if it needs
    to to catch up to the player’s clock.
    
    * It adapts to playing both too slowly and too fast. As long as the game can update in real
      time, the game won’t fall behind. If the player’s machine is top-of-the-line, it will respond
      with a smoother gameplay experience.
    
    * It’s more complex. The main downside is there is a bit more going on in the implementation.
      You have to tune the update time step to be both as small as possible for the high-end,
      while not being too slow on the low end.

Writing a game with Possum will shield you from these gory details, but it may be of general interest
if you're going deeper down the rabbit hole.

## Input

Raw input is collected via the `InputAdapter` interface and passed to any `InputContext`s that the game
has attached to the active `Scene` for mapping into a higher-level `MappedInput` event. This allows
an input context to map simple keypresses to events (KeyEvent.VK_UP to MappedInput.UP, for example) or
collect multiple keypress combinations into higher-level actions with semantic meaning to the game.
These are then set as the `Scene#activeInput` for each frame, and any `GameSystem`s active will
have a chance to react to them.

The provided `InputAdapter` implementation uses AWT's `KeyListener`.

## Rendering Process (AsciiPanelRenderingSystem)

### Overview
The `Scene` object contains a list of game entities. During each main loop iteration,
the `RenderingSystem`(s) are asked to render the scene. `RenderingSystem` is currently a
fairly non-specific interface, so in theory each renderer could come up with its own rendering
strategy. At present, however, `AsciiPanelRenderingSystem` is the only system implemented, and
with the implementation of future renderers it's quite likely I'll extract many of the useful
mechanisms from the AsciiPanel code into a more general class for all renderers to leverage.
But in the meantime, I'll describe how `AsciiPanelRenderingSystem` works.

### Active rendering
Rather than coupling visible entities to `AWT`'s passive rendering strategy (the Event Dispatch
Thread calls `paint()` on components as it sees fit), we take active control of determining
what screen regions need to be redrawn during each render loop. This also allows us to use more
advanced rendering techniques than would be available passively.

### Scroll check
When `RenderingSystem#render()` is called, we first check that the entity with camera
focus has not moved beyond the edge of our defined scroll boundaries; if so, we clear the entire
viewport and re-render everything including `PanelEntity`s and their contents. There is some
efficiency to be gained here in the future by not re-rendering panels, but in any case the logic
for rendering after scrolling will proceed normally as follows below.

### Entities to be rendered
When an action occurs which requires an entity to be re-rendered (the user moved their character, we
scrolled the screen, etc.), the `GameSystem` which determined this will call `Scene#entityNeedsRendering()`.
The `Scene` maintains a list of 'dirty' entities to render for the next iteration. It doesn't
do the rendering itself, it merely tracks this list for any interested `RenderingSystem` to act on.

## Random number generation

A shared pseudo-random number generator is used throughout, and is seeded with the current timestamp
by default. To facilitate repeatable behavior when testing, you can specify a random seed by
adding it to an `application.yml` file in the game's working directory like so:

    random-seed: 1
