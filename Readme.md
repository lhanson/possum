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

# TODO

Everything. This will probably be abandoned soon after creation, but here's
a short list:

    * Main event loop
    * Decoupled messaging paradigm to avoid direct dependencies between components/modules
    * Determine component breakdown
