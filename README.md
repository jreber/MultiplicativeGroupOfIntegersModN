# mult-mod

My investigations into the [multiplicative group of integers modulo
n](http://mathworld.wolfram.com/ModuloMultiplicationGroup.html)

## Workbook-Based Explorations and Discussions

* [Creating Cycles In The Multiplicative Group of Integers Modulo
  N](http://viewer.gorilla-repl.org/view.html?source=github&user=jreber&repo=mult-mod&path=src/mult_mod/workbooks/create_basic_cycles.clj)

## Background
What is the multiplicative group of integers modulo n? To understand
that, start with an arbitrary number:

![Number seven](https://github.com/jreber/mult-mod/raw/master/doc/intro/number-seven.png)

Now iteratively exponentiate that number.

![Number seven iteratively
exponentiated](https://github.com/jreber/mult-mod/raw/master/doc/intro/seven-iteratively-exponentiated.png)

Straightforward: consecutive exponential values. What if I take all
those numbers modulo an arbitrary number, like 17?

![Number seven iteratively exponentiated, modulo
17](https://github.com/jreber/mult-mod/raw/master/doc/intro/seven-iteratively-exponentiated-mod-17.png)

Whoa, what? An exponential sequences of numbers, when modulo an
arbitrary number, forms a cycle?

* Where'd the cycle come from? We were
  talking exponetials growing to infinity, not cycles. Sure, [certain
  types of exponetial numbers are equivalent with
  cycles](https://en.wikipedia.org/wiki/Euler%27s_identity), but is that
  at all related with what's going on here?
* How do these cycles change as you change the modulo base? What is
  the function defining how `modulo 16` cycles differs from `modulo
  17` cycles, or generally how `modulo n` cycles differ from `modulo
  (n + 1)` cycles?
* Is it possible to define the cycle for arbitrary parameters? Is
  there some way to say, "I know what `7^9,531,207 modulo 1,209,513`
  is" without exhaustively exponentiating and modulo-ing?

Ok, back to the cycles at hand. What if we try it again, this time
with `9^x modulo 17`?

![Number nine iteratively exponentiated, modulo
17](https://github.com/jreber/mult-mod/raw/master/doc/intro/nine-iteratively-exponentiated-mod-17.png)

The cycle still occurs, and it has an interesting relation with the
cycle for `7^x modulo 17`: If you traverse the `7^x modulo 17` cycle
starting at `x = 1` (node value of `1`) and go to the sixth element of
the cycle, you get the same cycle as `9^x modulo 17`. Put in
mathematical notation, if `f[x] = 7^x modulo 17` and `g[x] = 9^x
modulo 17` (`x >= 0`), then `g[x] = f[6x]`. Why? (That's basic
algebra; I'll figure that out shortly.)

I know there are group theory and number theory explanations for these
phenomena but I don't yet know those branches of mathematics — I'm a
software engineer who has gotten an intuitive-math boost thanks to
[3blue1brown](https://www.3blue1brown.com/). And while I want to know
the group theory and number theory at play here, I really want an
intuitive understanding of why composing exponentiation and modulus
results in cycles (even if that explanation ends up being, "Modulus is
by definition a cycle and multiplcation is deterministic, so of course
you'll eventually get to a number that you've seen before.").

This project is the playground for me to play around with these cycles
to better understand them.

## Installation

Clone from GitHub. You must have Leiningen installed.

## Usage

I'll get to this. For now, just run it from the REPL. See my REPL
examples in the files.

## License

Copyright © 2019 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
