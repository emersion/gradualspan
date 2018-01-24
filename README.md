# gradualspan

[![Build Status](https://travis-ci.org/emersion/gradualspan.svg?branch=master)](https://travis-ci.org/emersion/gradualspan)

Research paper, published at EGC 2018 (in French): http://www.editions-rnti.fr/?inprocid=1002382

## Usage

Requires Java 8 and Maven.

Run with:

```
make run
```

This outputs graphs in `valued-sequences.dot`, `gradual-sequences.dot` and
`patterns.dot`. To render these graphs, use `bin/multidot <filename>` (requires
Graphviz to be installed).

## Implementing your own backend

In this repository, two backends are implemented: a memory backend for unit
tests and a [PO²](http://agroportal.lirmm.fr/ontologies/PO2) backend.

This implementation of GradualSpan is modular, you can implement you own backend
pretty easily as long as you have a partially ordered valued sequence database.
To do so, you need to implement the following interfaces: `ValuedItem`,
`ValuedNode` and `ValuedSequence`.

## License

MIT
