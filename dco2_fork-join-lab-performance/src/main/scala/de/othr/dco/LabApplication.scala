package de.othr.dco

import com.github.kiprobinson.bigfraction.BigFraction

import java.math.BigInteger
import java.util.Random


object LabApplication extends App {

  val MaxNumbers = 10000

  val numbers = LazyList.continually( ( new BigInteger(150000, new Random()), new BigInteger(15000, new Random()) ) )
    .take(MaxNumbers)
    .toArray

  // Have fun :-)

}
