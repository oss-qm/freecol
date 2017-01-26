# Optimized and j7-backported version of FreeCol

This is an optimized and cleaned-up branch of FreeCol, which also
works fine on jre-7, so - in contrast to upstream - also runs on
stable distros (eg. Debian Jessie or Ubuntu Precise) without
installing an untrusted blob of jre-8.

Key goals are: (order of the patch stacks)

* optimize-away expensive, resource burning, code - which usally
  happen to be complicated java8-only things like pseudo-"functional"
  constructs like "Streams" (which have actually have nothing to do
  with functional programming at all :o), needlessly complicated
  chains of callback classes, etc, etc.

* replace (the after #1 few) remaining jre-8 by simpler jre-7-
  compatible code (eg. abuse of workarounds like "default interface
  methods", etc)

* drop bundled precompiled binaries

* packaging for Debian (oder -based distros)


# freecol

FreeCol is a turn-based strategy game based on the old game
Colonization, and similar to Civilization. The objective of the game is
to create an independent nation.

You start with only a few colonists defying the stormy seas in their
search for new land. Will you guide them on the Colonization of a New
World?

## About FreeCol

The FreeCol team aims to create an Open Source version of Colonization
(released under the GPL). At first we'll try to make an exact clone of
Colonization. The visuals will be brought up to date with more recent
standards but will remain clean, simple and functional. Certain new
'features' will be implemented but the gameplay and the rules will be
exactly the same as the original game. Examples of modern features are:
an isometric map and multiplayer support.

This clone will be developed incrementally and result in FreeCol 1.0.0
which will be an almost exact Colonization clone. Incremental
development basically means that we'll add features one at a time. This
allows us to have a running program at all times and also to release an
unfinished but working game once in a while.

Once FreeCol 1.0.0 is finished we'll start working towards FreeCol
2.0.0. FreeCol 2 will go beyond the original Colonization and will have
many new features, it will be an implementation of our (and our users')
image of what Colonization 2 would have been.
