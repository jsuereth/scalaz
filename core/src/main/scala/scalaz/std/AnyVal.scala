package scalaz
package std

import scalaz._

trait AnyValInstances {

  implicit val unitInstance: Group[Unit] with Order[Unit] with Show[Unit] = new Group[Unit] with Order[Unit] with Show[Unit] {
    def show(f: Unit) = ().toString.toList

    def append(f1: Unit, f2: => Unit) = ()

    def zero = ()

    def inverse(f:Unit) = ()

    def order(x: Unit, y: Unit) = Ordering.EQ

    override def equalIsNatural: Boolean = true
  }

  implicit object booleanInstance extends Order[Boolean] with Show[Boolean] {
    def show(f: Boolean) = f.toString.toList

    def order(x: Boolean, y: Boolean) = if (x < y) Ordering.LT else if (x == y) Ordering.EQ else Ordering.GT

    override def equalIsNatural: Boolean = true

    object conjunction extends Monoid[Boolean] {
      def append(f1: Boolean, f2: => Boolean) = f1 && f2

      def zero: Boolean = true
    }

    object disjunction extends Monoid[Boolean] {
      def append(f1: Boolean, f2: => Boolean) = f1 || f2

      def zero = false
    }

  }

  import Tags.{Conjunction, Disjunction}

  implicit val booleanDisjunctionNewTypeInstance: Monoid[Boolean @@ Disjunction] with Order[Boolean @@ Disjunction] = new Monoid[Boolean @@ Disjunction] with Order[Boolean @@ Disjunction] {
    def append(f1: Boolean @@ Disjunction, f2: => Boolean @@ Disjunction) = Disjunction(f1 || f2)

    def zero: Boolean @@ Disjunction = Disjunction(false)

    def order(a1: Boolean @@ Disjunction, a2: Boolean @@ Disjunction) = Order[Boolean].order(a1, a2)
  }

  implicit val booleanConjunctionNewTypeInstance: Monoid[Boolean @@ Conjunction] with Order[Boolean @@ Conjunction] = new Monoid[Boolean @@ Conjunction] with Order[Boolean @@ Conjunction] {
    def append(f1: Boolean @@ Conjunction, f2: => Boolean @@ Conjunction) = Conjunction(f1 && f2)

    def zero: Boolean @@ Conjunction = Conjunction(true)

    def order(a1: Boolean @@ Conjunction, a2: Boolean @@ Conjunction) = Order[Boolean].order(a1, a2)

  }

  implicit val byteInstance: Monoid[Byte] with Order[Byte] with Show[Byte] = new Monoid[Byte] with Order[Byte] with Show[Byte] {
    def show(f: Byte) = f.toString.toList

    def append(f1: Byte, f2: => Byte) = (f1 + f2).toByte

    def zero: Byte = 0

    def order(x: Byte, y: Byte) = if (x < y) Ordering.LT else if (x == y) Ordering.EQ else Ordering.GT

    override def equalIsNatural: Boolean = true

    object multiplication extends Monoid[Byte] {
      def append(f1: Byte, f2: => Byte) = (f1 * f2).toByte

      def zero: Byte = 1
    }

  }

  import Tags.{Multiplication}

  implicit val byteMultiplicationNewType: Monoid[Byte @@ Multiplication] with Order[Byte @@ Multiplication] = new Monoid[Byte @@ Multiplication] with Order[Byte @@ Multiplication] {
    def append(f1: Byte @@ Multiplication, f2: => Byte @@ Multiplication) = Multiplication((f1 * f2).toByte)

    def zero: Byte @@ Multiplication = Multiplication(1)

    def order(a1: Byte @@ Multiplication, a2: Byte @@ Multiplication) = Order[Byte].order(a1, a2)

    override def equalIsNatural: Boolean = true

  }

  implicit val char: Monoid[Char] with Order[Char] with Show[Char] = new Monoid[Char] with Order[Char] with Show[Char] {
    def show(f: Char) = f.toString.toList

    def append(f1: Char, f2: => Char) = (f1 + f2).toChar

    def zero: Char = 0

    def order(x: Char, y: Char) = if (x < y) Ordering.LT else if (x == y) Ordering.EQ else Ordering.GT

    override def equalIsNatural: Boolean = true

    object multiplication extends Monoid[Char] {
      def append(f1: Char, f2: => Char) = (f1 * f2).toChar

      def zero: Char = 1
    }

  }

  implicit val charMultiplicationNewType: Monoid[Char @@ Multiplication] with Order[Char @@ Multiplication] = new Monoid[Char @@ Multiplication] with Order[Char @@ Multiplication] {
    def append(f1: Char @@ Multiplication, f2: => Char @@ Multiplication) = Multiplication((f1 * f2).toChar)

    def zero: Char @@ Multiplication = Multiplication(1)

    def order(a1: Char @@ Multiplication, a2: Char @@ Multiplication) = Order[Char].order(a1, a2)

    override def equalIsNatural: Boolean = true
  }

  implicit val shortInstance: Group[Short] with Order[Short] with Show[Short] = new Group[Short] with Order[Short] with Show[Short] {
    def show(f: Short) = f.toString.toList

    def append(f1: Short, f2: => Short) = (f1 + f2).toShort

    def zero: Short = 0

    def inverse(f:Short) = (-f).toShort

    def order(x: Short, y: Short) = if (x < y) Ordering.LT else if (x == y) Ordering.EQ else Ordering.GT

    override def equalIsNatural: Boolean = true

    object multiplication extends Monoid[Short] {
      def append(f1: Short, f2: => Short) = (f1 * f2).toShort

      def zero: Short = 1
    }

  }

  implicit val shortMultiplicationNewType: Monoid[Short @@ Multiplication] with Order[Short @@ Multiplication] = new Monoid[Short @@ Multiplication] with Order[Short @@ Multiplication] {
    def append(f1: Short @@ Multiplication, f2: => Short @@ Multiplication) = Multiplication((f1 * f2).toShort)

    def zero: Short @@ Multiplication = Multiplication(1)

    def order(a1: Short @@ Multiplication, a2: Short @@ Multiplication) = Order[Short].order(a1, a2)
  }

  implicit val intInstance: Group[Int] with Order[Int] with Show[Int] = new Group[Int] with Order[Int] with Show[Int] {
    def show(f: Int) = f.toString.toList

    def append(f1: Int, f2: => Int) = f1 + f2

    def zero: Int = 0

    def inverse(f:Int) = -f

    def distance(a: Int, b: Int): Int = b - a

    def order(x: Int, y: Int) = if (x < y) Ordering.LT else if (x == y) Ordering.EQ else Ordering.GT

    override def equalIsNatural: Boolean = true

    object multiplication extends Monoid[Int] {
      def append(f1: Int, f2: => Int) = f1 * f2

      def zero: Int = 1
    }
  }

  /** Warning: the triangle inequality will not hold if `b - a` overflows. */
  implicit val intMetricSpace: MetricSpace[Int] = new MetricSpace[Int] {
    def distance(a: Int, b: Int): Int = scala.math.abs(b - a)
  }

  implicit val intMultiplicationNewType: Monoid[Int @@ Multiplication] with Order[Int @@ Multiplication] = new Monoid[Int @@ Multiplication] with Order[Int @@ Multiplication] {
    def append(f1: Int @@ Multiplication, f2: => Int @@ Multiplication) = Multiplication(f1 * f2)

    def zero: Int @@ Multiplication = Multiplication(1)

    def order(a1: Int @@ Multiplication, a2: Int @@ Multiplication) = Order[Int].order(a1, a2)
  }

  implicit val longInstance: Group[Long] with Order[Long] with Show[Long] = new Group[Long] with Order[Long] with Show[Long] {
    def show(f: Long) = f.toString.toList

    def append(f1: Long, f2: => Long) = f1 + f2

    def zero: Long = 0L

    def inverse(f: Long) = -f

    def order(x: Long, y: Long) = if (x < y) Ordering.LT else if (x == y) Ordering.EQ else Ordering.GT

    override def equalIsNatural: Boolean = true

    object multiplication extends Monoid[Long] {
      def append(f1: Long, f2: => Long) = f1 * f2

      def zero: Long = 1
    }

  }

  implicit val longMultiplicationNewType: Monoid[Long @@ Multiplication] with Order[Long @@ Multiplication] = new Monoid[Long @@ Multiplication] with Order[Long @@ Multiplication] {
    def append(f1: Long @@ Multiplication, f2: => Long @@ Multiplication) = Multiplication(f1 * f2)

    def zero: Long @@ Multiplication = Multiplication(1)

    def order(a1: Long @@ Multiplication, a2: Long @@ Multiplication) = Order[Long].order(a1, a2)
  }

  implicit val floatInstance: Group[Float] with Order[Float] with Show[Float] = new Group[Float] with Order[Float] with Show[Float] {
    def show(f: Float) = f.toString.toList

    def append(f1: Float, f2: => Float) = f1 + f2

    def zero: Float = 0f

    def inverse(f: Float) = -f

    override def equalIsNatural: Boolean = true

    def order(x: Float, y: Float) = if (x < y) Ordering.LT else if (x == y) Ordering.EQ else Ordering.GT
  }

  implicit val floatMultiplicationNewType: Group[Float @@ Multiplication] = new Group[Float @@ Multiplication] {
    def append(f1: Float @@ Multiplication, f2: => Float @@ Multiplication) = Multiplication(f1 * f2)

    def zero: Float @@ Multiplication = Multiplication(1.0f)

    def inverse(f: Float @@ Multiplication) = Multiplication(1.0f/f)

  }

  implicit val doubleInstance: Group[Double] with Order[Double] with Show[Double] = new Group[Double] with Order[Double] with Show[Double] {
    def show(f: Double) = f.toString.toList

    def append(f1: Double, f2: => Double) = f1 + f2

    def zero: Double = 0d

    def inverse(f: Double) = -f

    def order(x: Double, y: Double) = if (x < y) Ordering.LT else if (x == y) Ordering.EQ else Ordering.GT

    override def equalIsNatural: Boolean = true
  }

  implicit val doubleMultiplicationNewType: Group[Double @@ Multiplication] = new Group[Double @@ Multiplication] {
    def append(f1: Double @@ Multiplication, f2: => Double @@ Multiplication) = Multiplication(f1 * f2)

    def zero: Double @@ Multiplication = Multiplication(1.0d)

    def inverse(f: Double @@ Multiplication) = Multiplication(1.0d/f)
  }
}

trait BooleanFunctions {

  /**
   * Conjunction. (AND)
   *
   * {{{
   * p q  p ∧ q
   * 0 0  0
   * 0 1  0
   * 1 0  0
   * 1 1  1
   * }}}
   */
  final def conjunction(p: Boolean, q: => Boolean) = p && q

  /**
   * Disjunction. (OR)
   *
   * {{{
   * p q  p ∨ q
   * 0 0  0
   * 0 1  1
   * 1 0  1
   * 1 1  1
   * }}}
   */
  final def disjunction(p: Boolean, q: => Boolean) = p || q

  /**
   * Negation of Conjunction. (NOR)
   *
   * {{{
   * p q  p !&& q
   * 0 0  1
   * 0 1  1
   * 1 0  1
   * 1 1  0
   * }}}
   */
  final def nor(p: Boolean, q: => Boolean) = !p || !q

  /**
   * Negation of Disjunction. (NAND)
   *
   * {{{
   * p q  p !|| q
   * 0 0  1
   * 0 1  0
   * 1 0  0
   * 1 1  0
   * }}}
   */
  final def nand(p: Boolean, q: => Boolean) = !p && !q

  /**
   * Conditional.
   *
   * {{{
   * p q  p --> q
   * 0 0  1
   * 0 1  1
   * 1 0  0
   * 1 1  1
   * }}}
   */
  final def conditional(p: Boolean, q: => Boolean) = !p || q

  /**
   * Inverse Conditional.
   *
   * {{{
   * p q  p <-- q
   * 0 0  1
   * 0 1  0
   * 1 0  1
   * 1 1  1
   * }}}
   */
  final def inverseConditional(p: Boolean, q: => Boolean) = p || !q

  /**
   * Negational of Conditional.
   *
   * {{{
   * p q  p ⇏ q
   * 0 0  0
   * 0 1  0
   * 1 0  1
   * 1 1  0
   * }}}
   */
  final def negConditional(p: Boolean, q: => Boolean) = p && !q

  /**
   * Negation of Inverse Conditional.
   *
   * {{{
   * p q  p <\- q
   * 0 0  0
   * 0 1  1
   * 1 0  0
   * 1 1  0
   * }}}
   */
  final def negInverseConditional(p: Boolean, q: => Boolean) = !p && q


  /**
   * Executes the given side-effect if `cond` is `false`
   */
  final def unless(cond: Boolean)(f: => Unit) = if (!cond) f

  /**
   * Executes the given side-effect if `cond` is `true`
   */
  final def when(cond: Boolean)(f: => Unit) = if (cond) f

  /**
   * @return `t` if `cond` is `true`, `f` otherwise
   */
  final def fold[A](cond: Boolean, t: => A, f: => A): A = if (cond) t else f

  /**
   * Returns the given argument in `Some` if `cond` is `true`, `None` otherwise.
   */
  final def option[A](cond: Boolean, a: => A): Option[A] = if (cond) Some(a) else None

  /** Returns `1` if `p` is true, or `0` otherwise. */
  def test(p: Boolean): Int = if (p) 1 else 0

  /**
   * Returns the given argument if `cond` is `true`, otherwise, the zero element for the type of the given
   * argument.
   */
  final def valueOrZero[A](cond: Boolean)(value: => A)(implicit z: Monoid[A]): A = if (cond) value else z.zero

  /**
   * Returns the given argument if `cond` is `false`, otherwise, the zero element for the type of the given
   * argument.
   */
  final def zeroOrValue[A](cond: Boolean)(value: => A)(implicit z: Monoid[A]): A = if (!cond) value else z.zero

  /**
   * Returns the value `a` lifted into the context `M` if `cond` is `true`, otherwise, the empty value
   * for `M`.
   */
  final def pointOrEmpty[M[_], A](cond: Boolean)(a: => A)(implicit M: Pointed[M], M0: PlusEmpty[M]): M[A] =
    if (cond) M.point(a) else M0.empty

  /**
   * Returns the value `a` lifted into the context `M` if `cond` is `false`, otherwise, the empty value
   * for `M`.
   */
  final def emptyOrPure[M[_], A](cond: Boolean)(a: => A)(implicit M: Pointed[M], M0: PlusEmpty[M]): M[A] =
    if (!cond) M.point(a) else M0.empty

  final def pointOrEmptyNT[M[_]](cond: Boolean)(implicit M: Pointed[M], M0: PlusEmpty[M]): (Id ~> M) =
    new (Id ~> M) {
      def apply[A](a: A): M[A] = pointOrEmpty(cond)(a)
    }

  final def emptyOrPureNT[M[_]](cond: Boolean)(implicit M: Pointed[M], M0: PlusEmpty[M]): (Id ~> M) =
    new (Id ~> M) {
      def apply[A](a: A): M[A] = emptyOrPure(cond)(a)
    }
}

trait IntFunctions {
  def heaviside(i: Int) = if (i < 0) 0 else i
}

trait ShortFunctions {
  def heaviside(i: Short) = if (i < 0) 0 else i
}

trait LongFunctions {
  def heaviside(i: Long) = if (i < 0) 0 else i
}

trait DoubleFunctions {
  def heaviside(i: Double) = if (i < 0) 0 else i
}

trait FloatFunctions {
  def heaviside(i: Float) = if (i < 0) 0 else i
}

object anyVal extends AnyValInstances

object boolean extends BooleanFunctions

object short extends ShortFunctions

object int extends IntFunctions

object long extends LongFunctions

object double extends DoubleFunctions

object float extends FloatFunctions
