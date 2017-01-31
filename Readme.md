# Possum

[![Build Status](https://travis-ci.org/lhanson/possum.svg?branch=master)](https://travis-ci.org/lhanson/possum)
[![codecov](https://codecov.io/gh/lhanson/possum/branch/master/graph/badge.svg)](https://codecov.io/gh/lhanson/possum)
[![Dependency Status](https://www.versioneye.com/user/projects/584ea9225d8a550042585f1c/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/584ea9225d8a550042585f1c)
[![Dependency Status](https://dependencyci.com/github/lhanson/possum/badge)](https://dependencyci.com/github/lhanson/possum)

A trash-eating game engine.

![Let's eat trash and get hit by a car](https://s-media-cache-ak0.pinimg.com/736x/ca/20/41/ca20415ef281931b9bbf8abc7144d6ea.jpg)


# Overview

A major guiding principle behind Possum is that the game engine is decoupled
from UI concerns. That allows any given game to easily swap out UI approaches
without a ripple effect throughout the rest of the game, and ensures that
the engine design is abstracted well for a variety of use cases.


# Current features

* Interfaces to support a variety of underlying I/O technologies.
  Out of the box, support for [AsciiPanel](https://github.com/trystan/AsciiPanel)
  rendering and Swing keyboard input adapter.


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
