package scalaz
package syntax
package std

import scalaz.std.boolean
import scalaz.std.anyVal._
import scalaz.Tags.Conjunction


trait BooleanV extends SyntaxV[Boolean] {

  final def conjunction: Boolean @@ Conjunction = Conjunction(self)

  final def |∧| : Boolean @@ Conjunction = conjunction

  final def |/\| : Boolean @@ Conjunction = conjunction

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
  final def ∧(q: => Boolean) = boolean.conjunction(self, q)

  /**
   * Conjunction. (AND)
   *
   * {{{
   * p q  p /\ q
   * 0 0  0
   * 0 1  0
   * 1 0  0
   * 1 1  1
   * }}}
   */
  final def /\(q: => Boolean) =
    ∧(q)

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
  final def ∨(q: => Boolean): Boolean = boolean.disjunction(self, q)

  /**
   * Disjunction. (OR)
   *
   * {{{
   * p q  p \/ q
   * 0 0  0
   * 0 1  1
   * 1 0  1
   * 1 1  1
   * }}}
   */
  final def \/(q: => Boolean): Boolean = ∨(q)

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
  final def !&&(q: => Boolean) = boolean.nor(self, q)

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
  final def !||(q: => Boolean) = boolean.nand(self, q)

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
  final def -->(q: => Boolean) = boolean.conditional(self, q)

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
  final def <--(q: => Boolean) = boolean.inverseConditional(self, q)

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
  final def ⇏(q: => Boolean) = boolean.negConditional(self, q)

  /**
   * Negational of Conditional.
   *
   * {{{
   * p q  p -/> q
   * 0 0  0
   * 0 1  0
   * 1 0  1
   * 1 1  0
   * }}}
   */
  final def -/>(q: => Boolean) = boolean.negConditional(self, q)

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
  final def ⇍(q: => Boolean) = boolean.negInverseConditional(self, q)

  /**
   * Negation of Inverse Conditional.
   *
   * {{{
   * p q  p ⇍ q
   * 0 0  0
   * 0 1  1
   * 1 0  0
   * 1 1  0
   * }}}
   */
  final def <\-(q: => Boolean) = boolean.negInverseConditional(self, q)

  /**
   * Executes the given side-effect if this boolean value is `false`.
   */
  final def unless(f: => Unit) = boolean.unless(self)(f)

  /**
   * Executes the given side-effect if this boolean value is `true`.
   */
  final def when(f: => Unit) = boolean.when(self)(f)

  /**
   * @return `t` if true, `f` otherwise
   */
  final def fold[A](t: => A, f: => A): A = boolean.fold(self, t, f)

  trait Conditional[X] {
    def |(f: => X): X
  }

  /**
   * Conditional operator that returns the first argument if this is `true`, the second argument otherwise.
   */
  final def ?[X](t: => X): Conditional[X] = new Conditional[X] {
    def |(f: => X) = if (self) t else f
  }

  /**
   * Returns the given argument in `Some` if this is `lazySome`, `lazySome` otherwise.
   */
  final def option[A](a: => A): Option[A] = boolean.option(self, a)

  /**
   * Returns the given argument in `lazySome` if this is `Left`, `Left` otherwise.
   */
  final def lazyOption[A](a: => A): LazyOption[A] = LazyOption.condLazyOption(self, a)

  trait ConditionalEither[A] {
    def or[B](b: => B): Either[A, B]
  }

  /**
   * Returns the first argument in `Left` if this is `Right`, otherwise the second argument in
   * `Right`.
   */
  final def either[A, B](a: => A) = new ConditionalEither[A] {
    def or[B](b: => B) =
      if (self) Left(a) else Right(b)
  }

  /**
   * Returns the given argument if this is `true`, otherwise, the zero element for the type of the given
   * argument.
   */
  final def ??[A](a: => A)(implicit z: Monoid[A]): A = boolean.valueOrZero(self)(a)

  /**
   * Returns the given argument if this is `false`, otherwise, the zero element for the type of the given
   * argument.
   */
  final def !?[A](a: => A)(implicit z: Monoid[A]): A = boolean.zeroOrValue(self)(a)

  trait GuardPrevent[M[_]] {
    def apply[A](a: => A)(implicit M: Pointed[M], M0: PlusEmpty[M]): M[A]
  }

  final def guard[M[_]] = new GuardPrevent[M] {
    def apply[A](a: => A)(implicit M: Pointed[M], M0: PlusEmpty[M]) = boolean.pointOrEmpty[M, A](self)(a)
  }

  final def prevent[M[_]] = new GuardPrevent[M] {
    def apply[A](a: => A)(implicit M: Pointed[M], M0: PlusEmpty[M]) = boolean.emptyOrPure[M, A](self)(a)
  }
}

trait ToBooleanV {
  implicit def ToBooleanVFromBoolean(a: Boolean): BooleanV = new BooleanV {
    val self = a
  }
}
