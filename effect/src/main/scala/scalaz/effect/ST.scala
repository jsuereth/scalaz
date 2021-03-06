package scalaz
package effect

import RealWorld._
import STRef._
import STArray._
import ST._
import std.function._

/**Mutable variable in state thread S containing a value of type A. http://research.microsoft.com/en-us/um/people/simonpj/papers/lazy-functional-state-threads.ps.Z */
sealed trait STRef[S, A] {
  protected var value: A

  /**Reads the value pointed at by this reference. */
  def read: ST[S, A] = returnST(value)

  /**Modifies the value at this reference with the given function. */
  def mod[B](f: A => A): ST[S, STRef[S, A]] = st((s: World[S]) => {
    value = f(value);
    (s, this)
  })

  /**Associates this reference with the given value. */
  def write(a: => A): ST[S, STRef[S, A]] = st((s: World[S]) => {
    value = a;
    (s, this)
  })

  /**Synonym for write*/
  def |=(a: => A): ST[S, STRef[S, A]] =
    write(a)

  /**Swap the value at this reference with the value at another. */
  def swap(that: STRef[S, A]): ST[S, Unit] = for {
    v1 <- this.read
    v2 <- that.read
    _ <- this write v2
    _ <- that write v1
  } yield ()
}

object STRef extends STRefFunctions with STRefInstances {

  def apply[S]: (Id ~> ({type λ[α] = STRef[S, α]})#λ) =
    stRef[S]
}

trait STRefFunctions {

  def stRef[S]: (Id ~> ({type λ[α] = STRef[S, α]})#λ) = new (Id ~> ({type λ[α] = STRef[S, α]})#λ) {
    def apply[A](a: A) = new STRef[S, A] {
      var value = a
    }
  }
}

trait STRefInstances {
  
  /**Equality for STRefs is reference equality */
  implicit def STRefEqual[S, A]: Equal[STRef[S, A]] =
    Equal.equalA // todo reference equality?
}

/**Mutable array in state thread S containing values of type A. */
sealed trait STArray[S, A] {
  val size: Int
  val z: A
  implicit val manifest: Manifest[A]

  private val value: Array[A] = Array.fill(size)(z)

  import ST._

  /**Reads the value at the given index. */
  def read(i: Int): ST[S, A] = returnST(value(i))

  /**Writes the given value to the array, at the given offset. */
  def write(i: Int, a: A): ST[S, STArray[S, A]] = st(s => {
    value(i) = a;
    (s, this)
  })

  /**Turns a mutable array into an immutable one which is safe to return. */
  def freeze: ST[S, ImmutableArray[A]] = st(s => (s, ImmutableArray.fromArray(value)))

  /**Fill this array from the given association list. */
  def fill[B](f: (A, B) => A, xs: Traversable[(Int, B)]): ST[S, Unit] = xs match {
    case Nil             => returnST(())
    case ((i, v) :: ivs) => for {
      _ <- update(f, i, v)
      _ <- fill(f, ivs)
    } yield ()
  }

  /**Combine the given value with the value at the given index, using the given function. */
  def update[B](f: (A, B) => A, i: Int, v: B) = for {
    x <- read(i)
    _ <- write(i, f(x, v))
  } yield ()
}

object STArray extends STArrayFunctions {
  def apply[S, A](s: Int, a: A)(implicit m: Manifest[A]): STArray[S, A] = stArray(s, a)
}

trait STArrayFunctions {
  def stArray[S, A](s: Int, a: A)(implicit m: Manifest[A]): STArray[S, A] = new STArray[S, A] {
    val size = s
    val z = a
    implicit val manifest = m
  }
}

/**
 * Purely functional mutable state threads.
 * Based on JL and SPJ's paper "Lazy Functional State Threads"
 */
sealed trait ST[S, A] {
  private[effect] def apply(s: World[S]): (World[S], A)

  import ST._

  def flatMap[B](g: A => ST[S, B]): ST[S, B] =
    st(s => apply(s) match {
      case (ns, a) => g(a)(ns)
    })

  def map[B](g: A => B): ST[S, B] =
    st(s => apply(s) match {
      case (ns, a) => (ns, g(a))
    })
}

object ST extends STFunctions with STInstances {
  def apply[S, A](a: => A): ST[S, A] =
    returnST(a)
}

trait STFunctions {
  def st[S, A](f: World[S] => (World[S], A)): ST[S, A] = new ST[S, A] {
    private[effect] def apply(s: World[S]) = f(s)
  }

  // Implicit conversions between IO and ST
  implicit def STToIO[A](st: ST[RealWorld, A]): IO[A] =
    IO.io(rw => Free.return_(st(rw)))

  /**Put a value in a state thread */
  def returnST[S, A](a: => A): ST[S, A] =
    st(s => (s, a))

  /**Run a state thread */
  def runST[A](f: Forall[({type λ[S] = ST[S, A]})#λ]): A =
    f.apply.apply(realWorld)._2

  /**Allocates a fresh mutable reference. */
  def newVar[S]: (Id ~> ({type λ[α] = ST[S, STRef[S, α]]})#λ) = new (Id ~> ({type λ[α] = ST[S, STRef[S, α]]})#λ) {
    def apply[A](a: A) = returnST(stRef[S](a))
  }

  /**Allocates a fresh mutable array. */
  def newArr[S, A: Manifest](size: Int, z: A): ST[S, STArray[S, A]] =
    returnST(stArray[S, A](size, z))

  /**Allows the result of a state transformer computation to be used lazily inside the computation. */
  def fixST[S, A](k: (=> A) => ST[S, A]): ST[S, A] = st(s => {
    lazy val ans: (World[S], A) = k(r)(s)
    lazy val (_, r) = ans
    ans
  })

  /**Accumulates an integer-associated list into an immutable array. */
  def accumArray[F[_], A, B](size: Int, f: (A, B) => A, z: A, ivs: F[(Int, B)])(implicit F: Foldable[F], mf: Manifest[A]): ImmutableArray[A] = {
    import std.anyVal.unitInstance
    type STA[S] = ST[S, ImmutableArray[A]]
    runST(new Forall[STA] {
      def apply[S] = for {
        a <- newArr(size, z)
        _ <- {
          F.foldMap(ivs)((x: (Int, B)) => a.update(f, x._1, x._2))(stMonoid[S, Unit])
        }
        frozen <- a.freeze
      } yield frozen
    })
  }
}

trait STInstance0 {
  implicit def stSemigroup[S, A](implicit A: Semigroup[A]): Semigroup[ST[S, A]] =
      Monoid.liftSemigroup[({type λ[α] = ST[S, α]})#λ, A](ST.stMonad[S], A)
}

trait STInstances extends STInstance0 {
  implicit def stMonoid[S, A](implicit A: Monoid[A]): Monoid[ST[S, A]] =
    Monoid.liftMonoid[({type λ[α] = ST[S, α]})#λ, A](stMonad[S], A)

  implicit def stMonad[S]: Monad[({type λ[α] = ST[S, α]})#λ] = new Monad[({type λ[α] = ST[S, α]})#λ] {
    def point[A](a: => A): ST[S, A] = returnST(a)
    def bind[A, B](fa: ST[S, A])(f: (A) => ST[S, B]): ST[S, B] = fa flatMap f
  }
}
