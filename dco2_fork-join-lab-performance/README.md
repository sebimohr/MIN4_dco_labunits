# Fork-Join Pool Lab "Performance"

The class [BigFraction](https://github.com/kiprobinson/BigFraction) represents a fraction of two `BigInteger` objects. 
This fraction is reduced to the lowest term which (for big integer values of numerator and denominator) takes some time to compute.
For more details on `BigFraction` see [Kip Robinsons's Github Repo](https://github.com/kiprobinson/BigFraction).

Now consider a substantial number big integer values, that build the basis for the creation (and thus reduction) of `BigFraction` objects. 
The creation should again be done using a Fork/Join pool.

## Your Tasks

### Project setup

- The class [`LabApplication`](src/main/scala/de/othr/dco/LabApplication.scala) contains an array of random `BigInteger` objects
- Use these as the source of numerators and denominators for the creation of `BigFraction` objects within your fork/join task 
- Use `BigFraction::valueOf(BigInteger, BigInteger)` to create new objects

### Simple recursive approach

Define a class `BigFractionTask` that creates sub-tasks recursively and invoke it on a `ForkJoinPool` instance. 
The purpose of this task is to create the `BigFraction` objects. Creating the objects automatically reduces them (which is computing intensive). 
There is no need to store or return the new objects, we just need them to simulate computation intensive operations. 

### Generic recursive approach with performance review

It would be cool now to test different approaches and parameters and gain more insight.
For example, you could run an iterative test (without fork/join) followed by tests with different fork/join pool sizes and thresholds.
The number of steal counts that happen in the pools would be of interest, too. 

And check out your operating system's task list and watch how the CPU usage changes with every test.

A sample output of different test results could look like this:

```
 68052823618 | iterative                           |  
 20421063992 | fork/join(p/2)  threshold=1         | steal count=7 
 20648060721 | fork/join(p/2)  threshold=10        | steal count=11 
 20073481682 | fork/join(p/2)  threshold=100       | steal count=7 
 18195253569 | fork/join(p*2)  threshold=1         | steal count=99 
 18357321220 | fork/join(p*2)  threshold=10        | steal count=65 
 18136547322 | fork/join(p*2)  threshold=100       | steal count=64 
 18453099276 | fork/join(p-1)  threshold=1         | steal count=19 
 18530963420 | fork/join(p-1)  threshold=10        | steal count=28 
 18463054894 | fork/join(p-1)  threshold=100       | steal count=20 
 18610048247 | commonPool      threshold=1         | steal count=24 
 18375081904 | commonPool      threshold=10        | steal count=44 
 18177679447 | commonPool      threshold=100       | steal count=43 
```

Use your object-oriented and/or functional programming skills in Scala and see whether you can code without repeating too much code.

Of course, you could use unit tests here, but that is not required.

## Tipps and tricks

- `System.nanoTime()` return the current timestamp in nanoseconds since Epoch as a `Long` value (a nanosecond is a 1/1,000,000,000 of a second)  
- Right before you start your test, deliberately start the garbage collector with `System.gc`

## Useful resources

- The [Scala Library API documenation](https://www.scala-lang.org/api/2.13.6/)
- The [BigFraction](https://github.com/kiprobinson/BigFraction) Gitlab project site
- The Java™ Tutorials on [Fork/Join](https://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html)

