# macwire-test
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

A small study project on [macwire](https://github.com/adamw/macwire) a lightweight and nonintrusive Scala Dependency Injection library,
(if you only use the macro) by [softwaremill](http://www.softwaremill.com). The macwire library is released under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0).

## Introduction
Dependency Injection or DI for short is all about creating and managing an object graph that together form a component that solve
some kind of business problem. DI is also about loose coupling and inversion of control, programming against interfaces, making
dependencies explicit using constructor parameters to 'inject' dependencies and creating these dependent services outside of
your object graph, so for example in some kind of module that manages and wires these objects together to form the object graph!

Because macwire uses traits to create modules and it uses the the cake pattern light(tm), to wire these modules together
lets first discuss what mixins are, then traits, then the class/trait linearization problem
and lazy vals and potential deadlock problem using lazy vals before actually diving into macwire.

If you already know all of this then maybe you should take a look at
[DI in Scala Guide by Adam Warski](http://di-in-scala.github.io/) that explains [macwire](https://github.com/adamw/macwire)
and other DI subjects in depth, else read on!

## Motivation of using macwire
Basically the motivation is:

- we have Scala,
- I am confortable using traits,
- I know when to recognize a lazy val deadlock problem,
- I know how to create a good module stack and an object graph,
- I understand the lineair form of my modules (stack of traits, the cake),

then do I really need a __runtime__ do do dependency injection?

If we don't have a DI runtime/container readily available then macwire could be a good DI solution,
but if you create your application in eg. the [Playframework](https://www.playframework.com/), that has
the Guice DI runtime always running, then I'm not (yet) sold why you should use macwire in the playframework
because:

- you have [JSR-330](https://github.com/google/guice/wiki/JSR330),
- you have [Google Guice](https://github.com/google/guice) simplifies doing DI a lot,
- you already pay the DI runtime overhead (like class path scanning),
- you don't have to use lazy vals, which is a big plus in my book.

Moreover, macros are a bit magical as well, and for the developer, thats me, feel the same like the deus ex
approach of Guice or Spring for that matter. Lagom Scala even goes one step further and uses both Guice and macwire..

## Advantages using macwire
The following are the advantages using macwire:

- When having no DI runtime, and manually doing DI is tedious, then the `wire` macro can clean your code up,
- The code wiring code does not have to alter when the macro is managing the parameter list (adding/removing dependencies),
- Type-safety because the byte code is generated at compile time.

Lets understand some of the concepts that we need to use macwire effectively, read on!

## Mixin
Scala allows for __class member reuse__. For example, if you have a class member like eg `val age: Int = 42` or
`def doubleIt(x: Int): Int = 2 * x` and if you like to reuse these members in other classes then Scala has the
concept of a trait that allows you to do just that:

```scala
scala> trait ReuseMembers {
     | def doubleIt(x: Int): Int = 2 * x
     |  val age: Int = 42
     | }
defined trait ReuseMembers
```

The `trait` is a container for class member reuse and as such can only contain class members.
A trait is really an abstract class but one that cannot have constructor parameters.
Because a trait is abstract it cannot be instantiated:

```scala
scala> new ReuseMembers
<console>:13: error: trait ReuseMembers is abstract; cannot be instantiated
       new ReuseMembers
```

But you can give a trait a body, which makes it an anonymous class, then it can be instantiated:

```scala
scala> new ReuseMembers {}
res5: ReuseMembers = $anon$1@3e44f2a5

scala> res5.age
res6: Int = 42

scala> res5.doubleIt(2)
res7: Int = 4
```

What is the mixin part? Well, remember that a trait is a container for class members and that its reason for existence
is the reuse of those members? To reuse those members in other classes, we'll have to find a way to `mix-in` those
class members in a normal class:

```scala
scala> class Foo extends ReuseMembers
defined class Foo

scala> new Foo
res10: Foo = Foo@76318a7d

scala> res10.age
res11: Int = 42

scala> res10.doubleIt(2)
res12: Int = 4
```

The class `Foo` is just a simple class without any members. But wait, we have some members in our container
`ReuseMembers`, lets mix those into Foo! We can use the construct `class .. extends .. with .. with ..` for that.

We have now used the trait ReuseMembers as a mixin to the class Foo and as such have mixin the members `age` and `doubleIt`.

## Traits
A trait is a class that is meant to be added to some other class as a mixin. Unlike normal classes, traits cannot have constructor
parameters. Furthermore, no constructor arguments are passed to the superclass of the trait. This is not necessary as __traits are
initialized after the superclass is initialized__.

```scala
scala> trait Foo { println("Initializing Foo") }
defined trait Foo

scala> trait Bar {println("Initializing Bar") }
defined trait Bar

scala> trait Baz { println("Initializing Baz") }
defined trait Baz

scala> class Quz extends Foo with Bar with Baz
defined class Quz

scala> new Quz
Initializing Foo
Initializing Bar
Initializing Baz
res21: Quz = Quz@5348d83c
```

## Using traits as an interface/contract definition
A trait is an abstract construct and as such can contain abstract members. Abstract members are great because we
can use these abstract members to define a contract that the caller must abide to:

```scala
scala> trait Calc { def calc(x: Int): Int }
defined trait Calc
```

We can now create a trait that contains an implementation of this contract:

```scala
scala> trait DoubleCalc extends Calc { override def calc(x: Int): Int = 2*x }
defined trait DoubleCalc

scala> class Foo extends DoubleCalc
defined class Foo

scala> new Foo().calc(2)
res23: Int = 4
```

## Class Linearization in Scala
Scala supports only the single inheritance model bus because of mixins it is possible that we mix in multiple
inheritance graphs. Say that we have also defined a `QuadrupleCalc` trait, and we mix in both the `QuadrupleCalc`
and the `DoubleCalc` trait in our class Foo, how do we know what calc method will be used?

```scala
scala> trait Calc { def calc(x: Int): Int }
defined trait Calc

scala>

scala> trait DoubleCalc extends Calc { override def calc(x: Int): Int = 2*x }
defined trait DoubleCalc

scala>

scala> trait QuadrupleCalc extends Calc { override def calc(x: Int): Int = 4*x }
defined trait QuadrupleCalc

scala>

scala> class Foo extends QuadrupleCalc with DoubleCalc
defined class Foo

scala>

scala> new Foo().calc(2)
res22: Int = 4
```

To solve this problem Scala provides the [Class Linearization Algorithm](http://www.scala-lang.org/files/archive/spec/2.12/05-classes-and-objects.html#class-linearization)
also called 'Scala Linearization technique' or 'flatten-the-calls-to-super-classes', which is a deterministic process that
puts all traits in a `linear inheritance hierarchy` also called `lineair form`.

In Scala __every class__ has a lineair form eg:

```scala
class Foo

// lineair form
Foo -> AnyRef -> Any
```

When defining a simple class Foo, it implicitly extends the types AnyRef which extends Any.

Another way of putting this is that we can easily infer which class will be  our `super` from just reading the
source code.

Finding out the lineair form of the following object graph is also trivial:

```scala
scala> class Bar
defined class Bar

scala> class Foo extends Bar
defined class Foo

// lineair form
Foo -> Bar -> AnyRef -> Any
```

Our `super` is clearly Bar.

A more interesting problem is the one we started with:

```scala
class Foo extends QuadrupleCalc with DoubleCalc
```

Which is our super? Is it QuadrupleCalc or is it DoubleCalc? At first glance you would say QuadrupleCalc surely,
but when we evaluate `calc` with `2` we get the answer `4`, how so?

The answer is, when dealing with mixins, we must always determine the `lineair form` of the whole object graph so
what is the lineair form of `Foo`?

To anwer this we must understsand the 'Class Linearization Algorithm' which is a process Scala applies always when compiling
source code to come to a lineair form of any class hierarchy which flattens the calls to super classes. The algoritm is:

```
L(C) = C, L(Cn) +: ... +: L(C1)
```

Where L(C) is the 'lineair form of class C' and `+:` denotes a concatenation function where elements at right hand operand
replaces identical elements of the left hand operand, (we won't look at this now) and `C1... Cn` denotes the
inherited classes/traits in order they are declared for the class from left to right.

__Notice:__ Please notice that the formula starts with 'L(Cn) +: .. L(C1)', which means that when writing the lineair form
of the class, C1 is the first trait we have written down from left to right, C2 would be the second trait from the left
and so on. But when we write them down in the formula, we start with Cn, which is the last trait and so on, so the formula
is inverse to how we have written down the traits.

So in our case `Foo extends QuadrupleCalc with DoubleCalc` we would write the following to get the lineair form of
our object graph, we start with (Cn) which is the last trait we have written down which is 'DoubleCalc' and then C(1),
which is 'QuadrupleCalc':

```
L(Foo) = Foo, L(DoubleCalc) +: L(QuadrupleCalc)
```

But wait, we are not done yet, now we must first write out L(DoubleCalc) and L(QuadrupleCalc):

```
L(DoubleCalc) = DoubleCalc -> Calc -> AnyRef -> Any

L(QuadrupleCalc) = QuadrupleCalc -> Calc -> AnyRef -> Any
```

Lets fill L(DoubleCalc) and L(QuadrupleCalc) in our formula:

```
L(Foo) = Foo, DoubleCalc -> Calc -> AnyRef -> Any +: QuadrupleCalc -> Calc -> AnyRef -> Any
```

The algorithm is 'elements at the right hand operand replaces identical elements of the left hand operand',
which means that from the value: 'DoubleCalc -> Calc -> AnyRef -> Any +: QuadrupleCalc -> Calc -> AnyRef -> Any'
the operation '+:' will remove 'Calc -> AnyRef -> Any' from the left side and replace them with 'Calc -> AnyRef -> Any'
from the right side of the '+:' operation, so the lineair form of Foo will become:

```
L(Foo) = Foo -> DoubleCalc -> QuadrupleCalc -> Calc -> AnyRef -> Any
```

So our 'super' is DoubleCalc.

If we define Foo as follows:

```scala
scala> trait Calc { def calc(x: Int): Int }
defined trait Calc

scala>

scala> trait DoubleCalc extends Calc { override def calc(x: Int): Int = 2*x }
defined trait DoubleCalc

scala>

scala> trait QuadrupleCalc extends Calc { override def calc(x: Int): Int = 4*x }
defined trait QuadrupleCalc

scala>

scala> class Foo extends DoubleCalc with QuadrupleCalc
defined class Foo

scala>

scala> new Foo().calc(2)
res22: Int = 8
```

The lineair form would become:

```
L(Foo) = Foo -> L(QuadrupleCalc) +: L(DoubleCalc)

// which would be

L(Foo) = Foo -> QuadrupleCalc -> DoubleCalc -> Calc -> AnyRef -> Any
```

## Lazy vals
Now that we know about mixins, traits and class linearization, lets look at lazy vals. The lazy modifier applies to value definitions.
A lazy value (lazy val) is initialized the first time it is accessed (which might never happen at all).
Attempting to access a lazy value during its initialization might lead to looping behavior. If an exception is thrown during
initialization, the value is considered uninitialized, and a later access will retry to evaluate its right hand side.

Markus Hauck has done some research on this subject and explained it in his blog [Lazy Vals in Scala: A Look Under the Hood](https://blog.codecentric.de/en/2016/02/lazy-vals-scala-look-hood/),
lets listen to him:

The main characteristic of a lazy val is that the bound expression is not evaluated immediately, but once on the first access.
When the initial access happens, the expression is evaluated and the result bound to the identifier of the lazy val.
On subsequent access, no further evaluation occurs: instead the stored result is returned immediately.

Given the characteristic above, using the lazy modifier seems like an innocent thing to do, when we are defining a val,
why not also add a lazy modifier as a speculative “optimization”? In a moment we will see why this is typically not a good idea,
but before we dive into this, let’s recall the semantics of a lazy val first.

When we assign an expression to a lazy val like this:

```scala
scala> class Foo { lazy val two = 1 + 1 }
defined class Foo
```

we expect that the expression 1 + 1 is bound to two, but the expression is not yet evaluated. On the first
(and only on the first) access of two from somewhere else, the stored expression 1 + 1 is evaluated and the result
(2 in this case) is returned. On subsequent access of two, no evaluation happens: the stored result of the evaluation
was cached and will be returned instead.

This property of “evaluate once” is a very strong one. Especially if we consider a multithreaded scenario:
what should happen if two threads access our lazy val at the same time? Given the property that evaluation occurs only once,
we have to introduce some kind of synchronization in order to avoid multiple evaluations of our bound expression.
In practice, this means the bound expression will be evaluated by one thread, while the other(s) will have to wait
until the evaluation has completed, after which the waiting thread(s) will see the evaluated result.

A lazy val, other than a regular val, has to pay the cost of checking the initialization state on each access and
guarantee that initialization happens only once. The implementation is explained in [SIP-20](http://docs.scala-lang.org/sips/pending/improved-lazy-val-initialization.html)
but in short it uses `this.sychronized` and a `if..else` check for initializing the cached value.

Using lazy vals without giving it any more thought can lead to:

- sequential initialization due to monitor on instance
- deadlock on concurrent access of lazy vals without cycle
- deadlock in combination with other synchronization constructs

### Lazy val and safe forward references
Lazy vals are used for safe forward references, for example, the following code:

```scala
case class Foo()
case class Bar()
case class Quz(foo: Foo, bar: Bar)

val quz = Quz(foo, bar)
val bar = Bar()
val foo = Foo()

defined class Foo
defined class Bar
defined class Quz
quz: Quz = Quz(null,null)
bar: Bar = Bar()
foo: Foo = Foo()
```

Here foo and bar are forward referenced, and are not yet initialized so the Scala compiler will initialize them with
the [scala.Null](http://www.scala-lang.org/api/current/scala/Null.html)reference.

We can fix this by marking `bar` and `foo` as lazy:

```scala
case class Foo()
case class Bar()
case class Quz(foo: Foo, bar: Bar)

val quz = Quz(foo, bar)
lazy val bar = Bar()
lazy val foo = Foo()

defined class Foo
defined class Bar
defined class Quz
quz: Quz = Quz(Foo(),Bar())
bar: Bar = <lazy>
foo: Foo = <lazy>
```

### Lazy val deadlock problem
The lazy val deadlock problem is introduced when we have a cyclic dependency so a dependency that goes both ways when using
lazy vals. For example, say we have a Foo that depends on Bar and we have a Bar that depends on Foo so Foo <-> Bar:

```scala
case class Foo(b: Bar)
case class Bar(f: Foo)

lazy val foo: Foo = Foo(bar)
lazy val bar: Bar = Bar(foo)

defined class Foo
defined class Bar
foo: Foo = <lazy>
bar: Bar = <lazy>
```

Everything seems fine, until we access a member of either of them:

```scala
scala> foo.b
java.lang.StackOverflowError
 at ....
```

or

```scala
scala> bar.f
java.lang.StackOverflowError
  at ...
```

The best ways to fix this is by breaking the cyclic dependency, the Foo <-> Bar dependency. We can do that by introducing
a third type, lets call it Quz and put it between the two so Foo <- Quz -> Bar which means:

```scala
case class Foo()
case class Bar()
case class Quz(b: Bar, f: Foo)

lazy val quz: Quz = Quz(bar, foo)
lazy val foo: Foo = Foo()
lazy val bar: Bar = Bar()
```

Of course that means that we also have refactored the code in Foo and in Bar so that the logic that caused the cyclic dependency
is now in Quz. This of course means some refactoring and a change to our model. This goes to show how important it is to not
start coding immediately and first create a model...

Now its safe to call quz.b

```scala
scala> quz.b
res0: Bar = Bar()
```

Of course, cyclic dependencies are sometimes bad, like with object graphs as we saw here, and sometimes a good thing like when
creating a virtual object graph when referencing eg. entities by UUID when we want to reference an ID in
another DDD bounded context for example.

### Macwire lazy val initialization
When using macwire, the macwire macro can help us wiring the object graph together. We will use a combination of modeling
our object graph avoiding cyclic dependencies and using constructor parameters for classes. We don't have to bother ourselves
with the number of constructor arguments or the sequence in which they are defined:

Of course we need a dependency to macwire so the following must be in your `build.sbt`:

```bash
echo 'scalaVersion := "2.12.1"' >> build.sbt
echo 'libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"' >> build.sbt
```

Now we can use macwire eg. in the REPL so launch it typing `sbt console`:

```scala
:paste
class Foo()
class Bar()
class Quz(val f: Foo, val b: Bar)

lazy val quz: Quz = wire[Quz]
lazy val foo: Foo = wire[Foo]
lazy val bar: Bar = wire[Bar]

scala> quz.b
res0: Bar = Bar@9542531
```

## Cake Pattern Light
A [cake](https://en.wikipedia.org/wiki/Cake) is a form of sweet dessert that is typically baked. In its oldest forms,
cakes were modifications of breads, but cakes now cover a wide range of preparations that can be simple or elaborate,
and that share features with other desserts such as pastries, meringues, custards, and pies.

A cake has a filling which consists of a stack of layers of for example raspberry jam and lemon curd
and if you cut a cake you will see those layers one after another.

Like with a real cake, the cake pattern light also consists a stack of layers but here the layer is not raspberry jam
or lemon curd but traits:

```scala
scala> trait LemonCurd
defined trait LemonCurd

scala> trait RaspberryJam
defined trait RaspberryJam

scala> object Cake extends LemonCurd with RaspberryJam
defined object Cake
```

If you know Guice, then you know that you can define one or more Guice Modules that defines how to create an object graph
of a specific domain for example how to wire up a single component. In the cake pattern you can do the same but put
all the knowledge how to wire up a component in a trait. You can then assemble the 'Cake', also known as 'the whole application'
by mixing in all the traits which will then wire up all the dependencies.

So basically, the cake pattern light is for:

- Splitting up your dependency graph into modules (for example how to wire up a complete component),
- A module is implemented as a trait,
- An application consists of multiple modules (components) with (I would hope) zero dependencies between modules.
- You mix all these modules in your application which will launch your app.

## Macwire
I'm not going to explain macwire as Adam Warski already has done that perfectly so I would gladly redirect you to his
[blogpost](http://di-in-scala.github.io/).

## Documentation
- [Scala Specification - 5.2.8 - Lazy](http://www.scala-lang.org/files/archive/spec/2.12/05-classes-and-objects.html#lazy)
- [Scala Specification - 5.4 - Traits](http://www.scala-lang.org/files/archive/spec/2.12/05-classes-and-objects.html#traits)
- [Scala Specification - 5.1.2 - Class Linearization](http://www.scala-lang.org/files/archive/spec/2.12/05-classes-and-objects.html#class-linearization)
- [Class Linearization in Scala](http://cfchou.logdown.com/posts/241682-class-linearization-in-scala)
- [Constraining class linearization (mixin order) in Scala - eed3si9n](http://eed3si9n.com/constraining-class-linearization-in-Scala)
- [Lazy Vals in Scala: A Look Under the Hood](https://blog.codecentric.de/en/2016/02/lazy-vals-scala-look-hood/)
- [SIP-20 - Improved Lazy Vals Initialization](http://docs.scala-lang.org/sips/pending/improved-lazy-val-initialization.html)
- [Dependency injection vs. Cake pattern](http://www.cakesolutions.net/teamblogs/2011/12/15/dependency-injection-vs-cake-pattern)
- [Cake pattern in depth - Mark Harrison](http://www.cakesolutions.net/teamblogs/2011/12/19/cake-pattern-in-depth)

## Video
- [(0'52 hr) Scala Macros: What Are They, How Do They Work, and Who Uses Them? - Adam Warski](https://www.youtube.com/watch?v=iZjedoeOL00)
- [(0'47 hr) The no-framework Scala Dependency Injection Framework - Adam Warski](https://www.youtube.com/watch?v=JwKSUcXG7nw)
- [(0'34 hr) Macwire - Framework-less Scala Dependency Injection framework - Adam Waski](https://www.youtube.com/watch?v=n8a0A-m1w_c)
- [(0'27 hr) Approaches to Dependency Injection in Scala - Dave Gurnell](https://www.youtube.com/watch?v=OJe0Dm3t5wQ)
- [(1'06 hr) Implementing the Reactive Manifesto with Akka - Adam Warski](https://www.youtube.com/watch?v=LXEhQPEupX8)
- [(1'02 hr) Simple, fast & agile REST with Spray.io - Adam Warski](https://www.youtube.com/watch?v=XPuOlpWEvmw)
- [(0'59 hr) Streams: reactive? functional? Or: akka- & scalaz- streams side-by-side? - Adam Warski](https://www.youtube.com/watch?v=pXj5Q8KsXX0)
- [(0'39 hr) Transactional Event Sourcing using Slick audit log for free - Adam Warski](https://www.youtube.com/watch?v=RGqr1cXjS5o)

Have fun!