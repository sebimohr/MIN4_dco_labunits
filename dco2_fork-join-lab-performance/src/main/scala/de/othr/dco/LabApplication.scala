package de.othr.dco

import com.github.kiprobinson.bigfraction.BigFraction
import de.othr.dco.LabApplication.numbers

import java.math.BigInteger
import java.util.Random
import java.util.concurrent.{ForkJoinPool, ForkJoinTask, RecursiveAction}

object Constants {
  val THRESHOLD = 12
}

object LabApplication extends App {
  private val MaxNumbers = 10000
  private val numbers = LazyList.continually((new BigInteger(150000, new Random()), new BigInteger(15000, new Random())))
    .take(MaxNumbers)
    .toArray

  private val runStrategies = Array(0, 1, 5, 10, 50, 100, 1000)

  private val stringBuilder = new StringBuilder()
  runStrategies.foreach(runStrategy => {stringBuilder.append(TestBigFractionPerformance.InvokeForkJoinPool(runStrategy, numbers))})

  println(stringBuilder.toString())
}

object TestBigFractionPerformance {
  def InvokeForkJoinPool(threshold: Int, numbers: Array[(BigInteger, BigInteger)]): String = {
    assert(threshold >= 0, "Threshold must not be negative")

    val pool = new ForkJoinPool(Runtime.getRuntime.availableProcessors())
    val task = new BigFractionTask(numbers, 0, numbers.length - 1, threshold)

    val taskDescription = if (threshold == 0) "iterative" else "forkjoin"
    println(s"Invoking $taskDescription task.")

    System.gc()
    val startTime = System.nanoTime()
    if (threshold == 0) {
      numbers.foreach(num => {
        BigFraction.valueOf(num._1, num._2)
      })
    } else {
      pool.invoke(task)
    }
    val elapsedSeconds = System.nanoTime() - startTime

    s"${f"$elapsedSeconds%012d"} | $taskDescription \t threshold=${f"$threshold%05d"} \t| steal count = ${pool.getStealCount}\n"
  }
}

class BigFractionTask(array: Array[(BigInteger, BigInteger)], start: Int, end: Int, threshold: Int) extends RecursiveAction {
  override def compute(): Unit = {
    if (end - start < threshold) {
      (start to end).foreach { i => {
        BigFraction.valueOf(array(i)._1, array(i)._2)
      }
      }
    } else {
      val mid = start + (end - start) / 2

      val leftTask = new BigFractionTask(array, start, mid, threshold).fork()
      val rightTask = new BigFractionTask(array, mid + 1, end, threshold).fork()

      leftTask.join()
      rightTask.join()
    }
  }
}
