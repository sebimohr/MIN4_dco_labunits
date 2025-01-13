package de.othr.dco

import com.github.kiprobinson.bigfraction.BigFraction

import java.math.BigInteger
import java.util.Random
import java.util.concurrent.{ForkJoinPool, RecursiveAction}

object Constants {
  val THRESHOLD = 12
}

object LabApplication extends App {
  private val MaxNumbers = 10000

  private val numbers = LazyList.continually( ( new BigInteger(150000, new Random()), new BigInteger(15000, new Random()) ) )
    .take(MaxNumbers)
    .toArray

  println("Numbers are ready.")

  private val pool = new ForkJoinPool(Runtime.getRuntime.availableProcessors())
  private val task = new BigFractionTask(numbers, 0, numbers.length - 1)

  println("Invoking task.")

  pool.invoke(task)
}

class BigFractionTask(array: Array[(BigInteger, BigInteger)], start: Int, end: Int) extends RecursiveAction {
  override def compute(): Unit = {
    if (end - start < Constants.THRESHOLD) {
      (start to end).foreach { i => {
        BigFraction.valueOf(array(i)._1, array(i)._2)
      }}
      println(s"Computed ${end - start} numbers.")
    } else {
      println(s"Creating new tasks, numbers remaining: ${end - start}")
      val mid = start + (end - start) / 2

      val leftTask = new BigFractionTask(array, start, mid).fork()
      val rightTask = new BigFractionTask(array, mid + 1, end).fork()

      leftTask.join()
      rightTask.join()
    }
  }
}
