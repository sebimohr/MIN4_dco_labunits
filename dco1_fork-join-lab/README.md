# Fork-Join Pool Lab Unit

Consider a very big array of integer values that need to be manipulated,
lets say, every integer needs to be multiplied by 2. And this needs to be done using a fork-join pool.

## Your Tasks

### Project setup

- Create an `Array[Int]` (see tipps and tricks on how to do this in Scala)
- Instantiate a dedicated fork-join pool with a *parallism level* that reflects your system's number of processors/cores; class `Runtime` does provide this information

### Recursive approach with mutable array

- Define a class `RecursiveMutable` that creates sub-tasks recursively; the integers are directly manipulated in the given mutable array
- Make some experiments when to stop the recursive sub-tasking and note the differences in the overall time for the task
- Use class `RecursiveAction` here

### Recursive approach with immutable collections returned

- Define a class `RecursiveImmutable` that creates sub-tasks that return immutable collections of the manipulated integer values; the task should return a `List[Int]` with the computed integers
- Use class `RecursiveTask[V]` for your task definitions

### Iterative approach

- Define a class `IterativeMutable` that defines and creates sub-tasks in an iterative way
- Use class `RecursiveAction` here


### Additional Questions

- What is the difference between `ForkJoinPool::invoke(ForkJoinTask)`, `ForkJoinPool::execute(ForkJoinTask)`, and `ForkJoinPool::submit(ForkJoinTask)`?
- What is meant by the `ForkJoinPool::commonPool`?
- Can you define a task using native `ForkJoinTask[V]`? If yes, how (what is different)? If no, give a short explanation why not.


## Tipps and tricks

- "Classical" for-loops do not exist in Scala. Use `Range` instead with expressions like `val inclusiveRange = 1 to 100` or `val exclusiveRange = 1 until 100 by 2` and iterate through it using `for(elem <- range) expr` or `Range::foreach`
- Class `Range` does have a `toArray` method

## Useful resources

- The [Scala Library API documenation](https://www.scala-lang.org/api/2.13.6/)
- The Java™ Tutorials on [Fork/Join](https://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html)
- Twitter's [Scala School! From ∅ to Distributed Service](https://twitter.github.io/scala_school/)