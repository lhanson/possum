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
