package de.othr.dco

import RunStrategy.RunStrategy

import java.util.concurrent.{ForkJoinPool, RecursiveAction, RecursiveTask}
import scala.language.postfixOps

object RunStrategy extends Enumeration {
  type RunStrategy = Value
  val RECURSIVE_MUTABLE, RECURSIVE_IMMUTABLE, ITERATIVE_MUTABLE = Value
}


object LabApplication extends App {
  private val maxInteger = 100
  private val runStrategy = RunStrategy.RECURSIVE_MUTABLE
  private val printResult = false

  private def getTaskToExecute(task: RunStrategy) = task match {
    case RunStrategy.RECURSIVE_MUTABLE => new RecursiveMutable(array, 0, array.length - 1)
    case RunStrategy.RECURSIVE_IMMUTABLE => new RecursiveImmutable(List.from(array))
    case RunStrategy.ITERATIVE_MUTABLE => throw new NotImplementedError("Not yet implemented.")
  }

  /*
   Instantiate your fork-join-pool here,
   start task(s) and
   inform about the result (from) here
   */

  // Create ForkJoinPool with parallelism of available processors
  private val forkJoinPool = new ForkJoinPool(Runtime.getRuntime.availableProcessors())
  private val array = 1 to maxInteger toArray

  // Create task to execute in ForkJoinPool, then invoke it
  private val task = getTaskToExecute(runStrategy)
  private val result = forkJoinPool.invoke(task)

  if (printResult) {
    if (result.isInstanceOf[List[Int]]) {
      Iterable(result).foreach(println)
    } else {
      array.foreach(println)
    }
  }
}

class RecursiveMutable(array: Array[Int], start: Int, end: Int) extends RecursiveAction {
  // Threshold for single Task computation
  private val Threshold = 4

  override def compute(): Unit = {
    if (end - start < Threshold) {
      println(s"Doubling ${end - start} numbers in task.")
      (start to end).foreach(i => array(i) = array(i) * 2)
    } else {
      // Determine middle to split the array
      val middle = start + (end - start) / 2

      // Split task into 2 tasks
      println(s"Creating new pools for ${middle - start} Numbers each.")
      val leftTask = new RecursiveMutable(array, start, middle).fork()
      val rightTask = new RecursiveMutable(array, middle + 1, end).fork()

      // Join tasks back together
      leftTask.join()
      rightTask.join()
    }
  }
}

class RecursiveImmutable(list: List[Int]) extends RecursiveTask[List[Int]] {
  // Threshold for single Task computation
  private val Threshold = 4

  override def compute(): List[Int] = {
    if (list.length <= Threshold) {
      println(s"Doubling ${list.length} numbers in task.")
      list.map(_ * 2)
    }
    else {
      val mid = list.length / 2
      val (leftList, rightList) = list.splitAt(mid)

      println(s"Creating new pools for ${leftList.length} Numbers each.")
      val leftTask = new RecursiveImmutable(leftList).fork()
      val rightTask = new RecursiveImmutable(rightList).fork()

      // Join tasks back together
      leftTask.join() ::: rightTask.join()
    }
  }
}
