package scalaz
package std.java.math

import java.math.BigInteger

trait BigIntegerInstances {
  implicit val bigIntegerInstance: Group[BigInteger] with Order[BigInteger] with Show[BigInteger] = new Group[BigInteger] with Order[BigInteger] with Show[BigInteger] {
    def show(f: BigInteger) = f.toString.toList

    def append(f1: BigInteger, f2: => BigInteger) = f1 add f2

    def zero = BigInteger.ZERO

    def inverse(f: BigInteger) = f.negate()

    def order(x: BigInteger, y: BigInteger) = x.compareTo(y) match {
      case x if x < 0   => Ordering.LT
      case x if x == 0 => Ordering.EQ
      case x if x > 0   => Ordering.GT
    }

    object multiplication extends Monoid[BigInteger] {
      def append(f1: BigInteger, f2: => BigInteger) = f1 multiply f2

      def zero: BigInteger = BigInteger.ONE
    }
  }

  import Tags.Multiplication

  implicit val bigIntegerMultiplication: Monoid[BigInteger @@ Multiplication] with Order[BigInteger @@ Multiplication] with Show[BigInteger @@ Multiplication] = new Monoid[BigInteger @@ Multiplication] with Order[BigInteger @@ Multiplication] with Show[BigInteger @@ Multiplication] {
    def show(f: scalaz.@@[BigInteger, Multiplication]) = f.toString.toList

    def append(f1: BigInteger @@ Multiplication, f2: => BigInteger @@ Multiplication) = Multiplication(f1 multiply f2)

    def zero: BigInteger @@ Multiplication = Multiplication(BigInteger.ONE)

    def order(x: BigInteger @@ Multiplication, y: BigInteger @@ Multiplication) = x.compareTo(y) match {
      case x if x < 0   => Ordering.LT
      case x if x == 0 => Ordering.EQ
      case x if x > 0   => Ordering.GT
    }
  }
}

object bigInteger extends BigIntegerInstances {

}
