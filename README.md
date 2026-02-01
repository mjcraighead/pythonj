# pythonj - a simple, practical Python-to-Java compiler

`pythonj` is a compiler that runs a subset of **ordinary Python code** by translating it to
**Java** and executing it on the JVM.

Its primary goal is **semantic honesty**, with performance gains emerging where Python semantics
map cleanly onto a simpler execution model.

## What this is
- A **Python-to-Java compiler** with a strong bias toward **CPython-compatible behavior**
- A system that treats **dynamic Python semantics as intentional and first-class**
- A project focused on making **program meaning explicit**, not inferred
- A way to leverage an existing runtime (GC, JIT, threading) instead of reinventing one
- A vehicle for exploring how far Python semantics can map cleanly onto a simple, explicit object model

Unannotated Python code remains dynamic.
Annotations and restrictions are **opt-in** and **enforced**, never guessed.

## What this is *not*
- Not a CPython replacement
- Not a "better Python" language
- Not a speculative optimizer
- Not a system that infers intent or narrows semantics silently
- Not fully CPython-compatible at any cost
- Not committed to the JVM as a long-term identity

If preserving a CPython behavior would require replacing the current object and execution model
with one that is substantially more complex, less analyzable, or fundamentally incompatible
with future type-based reasoning, `pythonj` will reject it rather than emulate it.

## Design philosophy (short version)
- **Dynamic semantics are a valid choice**
- **Static semantics must be explicit**
- **The compiler must not guess**
- **Correctness and clarity come before performance**
- **Performance is a consequence of declared meaning**
- **Backends are an implementation detail**

If the compiler wants to make a stronger assumption than the program states, it must ask for
clarification — or not make the assumption.

## Why Java?
Java (and the JVM) provide:
- a mature garbage collector
- a high-quality JIT
- real parallelism (no GIL)
- a stable object model

This allows the project to focus on **semantics and mapping**, not runtime infrastructure.

Java is the current backend because it is practical — not because it is loved, and not because it is permanent.

## Scope and limitations
- Many Python features are unsupported or only partially supported
- Some CPython behaviors are intentionally excluded
- Standard library coverage is incomplete
- OS- and POSIX-heavy behavior is especially constrained on the JVM

These limitations are acknowledged design tradeoffs, not oversights.

## Requirements
This project currently requires:
- Python 3.x (most testing has been done on Python 3.14)
- Java 17+

There is no installation process yet.

## Status
This is an experimental, evolving project.
The README describes the *direction* more than the current implementation.

Large portions of Python syntax are implemented, but coverage is uneven.

Expect rough edges.

### One-sentence summary
> **`pythonj` exists to run Python code without pretending to understand what the program did not explicitly say.**

## License
Copyright © 2012-2026 Matt Craighead

Released under the terms of the MIT License — see [LICENSE](LICENSE) for details.
