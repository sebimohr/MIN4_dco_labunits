package de.othr.dco

import Constant.{THRESHOLD, MAX_INTEGER, PRINT_RESULT, RUN_STRATEGY}
import RunStrategy.RunStrategy

import java.util.concurrent.{ForkJoinPool, RecursiveAction, RecursiveTask}
import scala.language.postfixOps

object RunStrategy extends Enumeration {
  type RunStrategy = Value
  val RECURSIVE_MUTABLE, RECURSIVE_IMMUTABLE, ITERATIVE_MUTABLE = Value
}

object Constant {
  val THRESHOLD = 4
  val MAX_INTEGER = 1_000_000
  val RUN_STRATEGY: RunStrategy = RunStrategy.RECURSIVE_IMMUTABLE
  val PRINT_RESULT = false
}


object LabApplication extends App {
  private def getTaskToExecute = RUN_STRATEGY match {
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
  private val array = 1 to MAX_INTEGER toArray

  // Create task to execute in ForkJoinPool, then invoke it
  private val task = getTaskToExecute
  private val result = forkJoinPool.invoke(task)

  private def intMustBeEven(int: Int): Unit = {
    if (PRINT_RESULT)
      println(int)
    else if ((int % 2) > 0)
      println(s"$int has not been doubled!")

  }
  result match {
    case intList: List[Int] =>
      intList.foreach(intMustBeEven)
    case _ =>
      array.foreach(intMustBeEven)
  }
}

class RecursiveMutable(array: Array[Int], start: Int, end: Int) extends RecursiveAction {
  override def compute(): Unit = {
    if (end - start < THRESHOLD) {
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
  override def compute(): List[Int] = {
    if (list.length <= THRESHOLD) {
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

class IterativeMutable(array: Array[Int], start: Int, end: Int, private val isInternal: Boolean = false) extends RecursiveAction {
  // TODO: add constructor with only 3 input arguments for "mother" thread

  override def compute(): Unit = {
    // TODO: add iterative tasks that only compute internally
  }
}
