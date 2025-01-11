package de.othr.dco

import Constant.{MAX_INTEGER, PRINT_RESULT, RUN_STRATEGY, THRESHOLD}
import RunStrategy.RunStrategy

import java.util.concurrent.{ForkJoinPool, ForkJoinTask, RecursiveAction, RecursiveTask}
import scala.language.postfixOps

object RunStrategy extends Enumeration {
  type RunStrategy = Value
  val RECURSIVE_MUTABLE, RECURSIVE_IMMUTABLE, ITERATIVE_MUTABLE = Value
}

object Constant {
  val THRESHOLD = 12
  val MAX_INTEGER = 1_000_000
  val RUN_STRATEGY: RunStrategy = RunStrategy.RECURSIVE_IMMUTABLE
  val PRINT_RESULT = false
}


object LabApplication extends App {
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
  // NOTE: Invoke function returns T
  private val resultInvoke = forkJoinPool.invoke(task)

  // NOTE: Execute method only returns void
  //  private val resultExecute: Unit = forkJoinPool.execute(task)

  // NOTE: Submit method returns the ForkJoinTask<T>
  //  private val resultSubmit = forkJoinPool.submit(task)

  private def getTaskToExecute = RUN_STRATEGY match {
    case RunStrategy.RECURSIVE_MUTABLE => new RecursiveMutable(array, 0, array.length - 1)
    case RunStrategy.RECURSIVE_IMMUTABLE => new RecursiveImmutable(List.from(array))
    case RunStrategy.ITERATIVE_MUTABLE => new IterativeMutable(array, 0, array.length - 1)
  }

  private def intMustBeEven(int: Int): Unit = {
    if (PRINT_RESULT)
      println(int)
    else if ((int % 2) > 0)
      println(s"$int has not been doubled!")

  }

  resultInvoke match {
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

      // NOTE: Split task into 2 tasks, both manipulate the array
      println(s"Creating new pools for ${middle - start} Numbers each.")
      val leftTask = new RecursiveMutable(array, start, middle).fork()
      val rightTask = new RecursiveMutable(array, middle + 1, end).fork()

      // NOTE: Join tasks back together
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
      // NOTE: List is returned as result of RecursiveImmutable Task
      val leftTask = new RecursiveImmutable(leftList).fork()
      val rightTask = new RecursiveImmutable(rightList).fork()

      // NOTE: Join task lists back together and return them
      leftTask.join() ::: rightTask.join()
    }
  }
}

class IterativeMutable(array: Array[Int], start: Int, end: Int, private val isInternal: Boolean = false) extends RecursiveAction {
  def this(array: Array[Int], start: Int, end: Int) = {
    this(array, start, end, false)
  }

  assert(start >= 0 && start < array.length, "wrong start position")
  assert(end > 0 && end < array.length, "wrong end position")

  override def compute(): Unit = {
    if ((end - start) < THRESHOLD) {
      (start to end).foreach(i => array(i) = array(i) * 2)
    } else {
      var taskList: List[ForkJoinTask[Void]] = List()

      (0 until end by THRESHOLD).foreach {
        startPos =>
          val endPos = if (startPos + THRESHOLD >= array.length) array.length else startPos + THRESHOLD
          val task = new IterativeMutable(array, startPos, endPos - 1, true).fork()
          taskList = taskList :+ task
      }

      taskList.foreach(_.join())
    }
  }
}
