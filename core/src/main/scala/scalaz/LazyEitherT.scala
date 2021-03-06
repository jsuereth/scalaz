package scalaz

sealed trait LazyEitherT[F[_], A, B] {
  def run: F[LazyEither[A, B]]

  import LazyEither._
  import LazyEitherT._
  import EitherT.{LeftProjectionT => _, _}
  import OptionT._
  import LazyOptionT._

  def ?[X](left: => X, right: => X)(implicit F: Functor[F]): F[X] =
    F.map(run)(_.fold(_ => left, _ => right))

  def isLeft(implicit F: Functor[F]): F[Boolean] =
    F.map(run)(_.isLeft)

  def isRight(implicit F: Functor[F]): F[Boolean] =
    F.map(run)(_.isRight)

  def swap(implicit F: Functor[F]): F[LazyEither[B, A]] =
    F.map(run)(_.swap)

  def toEither(implicit F: Functor[F]): EitherT[F, A, B] =
    eitherT(F.map(run)(_.toEither))

  def getOrElse(default: => B)(implicit F: Functor[F]): F[B] =
    F.map(run)(_ getOrElse default)

  def exists(f: (=> B) => Boolean)(implicit F: Functor[F]): F[Boolean] =
    F.map(run)(_ exists f)

  def forall(f: (=> B) => Boolean)(implicit F: Functor[F]): F[Boolean] =
    F.map(run)(_ forall f)

  def orElse(x: => LazyEitherT[F, A, B])(implicit m: Bind[F]): LazyEitherT[F, A, B] = {
    val g = run
    LazyEitherT(m.bind(g)(_.fold(
      _ => x.run
      , _ => g
    )))
  }

  def toLazyOption(implicit F: Functor[F]): LazyOptionT[F, B] =
    lazyOptionT(F.map(run)(_ toLazyOption))

  def toOption(implicit F: Functor[F]): OptionT[F, B] =
    optionT(F.map(run)(_ toOption))

  def toList(implicit F: Functor[F]): F[List[B]] =
    F.map(run)(_ toList)

  def toStream(implicit F: Functor[F]): F[Stream[B]] =
    F.map(run)(_ toStream)

  def map[C](f: (=> B) => C)(implicit F: Functor[F]): LazyEitherT[F, A, C] =
    lazyEitherT(F.map(run)(_ map f))

  def foreach(f: (=> B) => Unit)(implicit e: Each[F]): Unit =
    e.each(run)(_ foreach f)

  def flatMap[C](f: (=> B) => LazyEitherT[F, A, C])(implicit M: Monad[F]): LazyEitherT[F, A, C] =
    lazyEitherT(M.bind(run)(_.fold(a => M.point(lazyLeft[C](a)), b => f(b).run)))

  def bimap[C, D](f: (=> A) => C, g: (=> B) => D)(implicit F: Functor[F]): LazyEitherT[F, C, D] =
    map(g).left.map(f)

  def bitraverse[G[_], C, D](f: (A) => G[C], g: (B) => G[D])(implicit F: Traverse[F], G: Applicative[G]): G[LazyEitherT[F, C, D]] = {
    import std.either.eitherInstance
    Applicative[G].map(F.traverse(run)(BiTraverse[LazyEither].bitraverseF(f, g)))(LazyEitherT(_: F[LazyEither[C, D]]))
  }

  def traverse[G[_], C](f: (B) => G[C])(implicit F: Traverse[F], G: Applicative[G]): G[LazyEitherT[F, A, C]] = {
    G.map(F.traverse(run)(o => LazyEither.lazyEitherInstance[A].traverse(o)(f)))(LazyEitherT(_))
  }

  def foldRight[Z](z: => Z)(f: (B, => Z) => Z)(implicit F: Foldable[F]): Z = {
    F.foldR[LazyEither[A, B], Z](run, z)(a => b => LazyEither.lazyEitherInstance[A].foldRight[B, Z](a, b)(f))
  }

  def ap[C](f: => LazyEitherT[F, A, B => C])(implicit F: Applicative[F]): LazyEitherT[F, A, C] = {
    // TODO check laziness
    LazyEitherT[F, A, C](F.map2(f.run, run)((ff: LazyEither[A, B => C], aa: LazyEither[A, B]) => LazyEither.lazyEitherInstance[A].ap(aa)(ff)))
  }

  def left: LeftProjectionT[F, A, B] = new LazyEitherT.LeftProjectionT[F, A, B]() {
    val lazyEitherT = LazyEitherT.this
  }
}

object LazyEitherT extends LazyEitherTFunctions with LazyEitherTInstances {
  def apply[F[_], A, B](a: F[LazyEither[A, B]]): LazyEitherT[F, A, B] =
    lazyEitherT(a)

  sealed trait LeftProjectionT[F[_], A, B] {
    def lazyEitherT: LazyEitherT[F, A, B]

    import OptionT._
    import LazyOptionT._

    def getOrElse(default: => A)(implicit F: Functor[F]): F[A] =
      F.map(lazyEitherT.run)(_.left getOrElse default)

    def exists(f: (=> A) => Boolean)(implicit F: Functor[F]): F[Boolean] =
      F.map(lazyEitherT.run)(_.left exists f)

    def forall(f: (=> A) => Boolean)(implicit F: Functor[F]): F[Boolean] =
      F.map(lazyEitherT.run)(_.left forall f)

    def orElse(x: => LazyEitherT[F, A, B])(implicit m: Bind[F]): LazyEitherT[F, A, B] = {
      val g = lazyEitherT.run
      LazyEitherT(m.bind(g)((z: LazyEither[A, B]) => z.fold(
        _ => g
        , _ => x.run
      )))
    }

    def toLazyOption(implicit F: Functor[F]): LazyOptionT[F, A] =
      lazyOptionT(F.map(lazyEitherT.run)(_.left toLazyOption))

    def toOption(implicit F: Functor[F]): OptionT[F, A] =
      optionT(F.map(lazyEitherT.run)(_.left toOption))

    def toList(implicit F: Functor[F]): F[List[A]] =
      F.map(lazyEitherT.run)(_.left toList)

    def toStream(implicit F: Functor[F]): F[Stream[A]] =
      F.map(lazyEitherT.run)(_.left toStream)

    def map[C](f: (=> A) => C)(implicit F: Functor[F]): LazyEitherT[F, C, B] =
      LazyEitherT(F.map(lazyEitherT.run)(_.left map f))

    def foreach(f: (=> A) => Unit)(implicit F: Each[F]): Unit =
      F.each(lazyEitherT.run)(_.left foreach f)

    def flatMap[C](f: (=> A) => LazyEitherT[F, C, B])(implicit M: Monad[F]): LazyEitherT[F, C, B] =
      LazyEitherT(M.bind(lazyEitherT.run)(_.fold(a => f(a).run, b => M.point(LazyEither.lazyRight[C](b)))))
  }

}

trait LazyEitherTInstances3 {
  implicit def lazyEitherTFunctor[F[_], L](implicit F0: Functor[F]) = new LazyEitherTFunctor[F, L] {
    implicit def F = F0
  }
  implicit def lazyEitherTLeftProjectionFunctor[F[_], L](implicit F0: Functor[F]) = new IsomorphismFunctor[({type λ[α] = LazyEitherT.LeftProjectionT[F, L, α]})#λ, ({type λ[α] = LazyEitherT[F, L, α]})#λ] {
    implicit def G = lazyEitherTFunctor[F, L]
    def iso = LazyEitherT.lazyEitherTLeftProjectionEIso2[F, L]
  }
}

trait LazyEitherTInstances2 extends LazyEitherTInstances3 {
  implicit def lazyEitherTPointed[F[_], L](implicit F0: Pointed[F]) = new LazyEitherTPointed[F, L] {
    implicit def F = F0
  }
  implicit def lazyEitherTLeftProjectionPointed[F[_], L](implicit F0: Pointed[F]) = new IsomorphismPointed[({type λ[α] = LazyEitherT.LeftProjectionT[F, L, α]})#λ, ({type λ[α] = LazyEitherT[F, L, α]})#λ] {
    implicit def G = lazyEitherTPointed[F, L]
    def iso = LazyEitherT.lazyEitherTLeftProjectionEIso2[F, L]
  }
}

trait LazyEitherTInstances1 extends LazyEitherTInstances2 {
  implicit def lazyEitherTApplicative[F[_], L](implicit F0: Applicative[F]) = new LazyEitherTApplicative[F, L] {
    implicit def F = F0
  }
  implicit def lazyEitherTLeftProjectionApplicative[F[_], L](implicit F0: Applicative[F]) = new IsomorphismApplicative[({type λ[α] = LazyEitherT.LeftProjectionT[F, L, α]})#λ, ({type λ[α] = LazyEitherT[F, L, α]})#λ] {
    implicit def G = lazyEitherTApplicative[F, L]
    def iso = LazyEitherT.lazyEitherTLeftProjectionEIso2[F, L]
  }  
}

trait LazyEitherTInstances0 extends LazyEitherTInstances1 {
  implicit def lazyEitherTBiFunctor[F[_]](implicit F0: Functor[F]) = new LazyEitherTBiFunctor[F] {
    implicit def F = F0
  }
  implicit def lazyEitherTLeftProjectionBiFunctor[F[_]](implicit F0: Functor[F]) = new IsomorphismBiFunctor[({type λ[α, β]=LazyEitherT.LeftProjectionT[F, α, β]})#λ, ({type λ[α, β]=LazyEitherT[F, α, β]})#λ] {
    implicit def G = lazyEitherTBiFunctor[F]
    def iso = LazyEitherT.lazyEitherTLeftProjectionIso2[F]
  }

  implicit def lazyEitherTMonad[F[_], L](implicit F0: Monad[F]) = new LazyEitherTMonad[F, L] {
    implicit def F = F0
  }
  implicit def lazyEitherTLeftProjectionMonad[F[_], L](implicit F0: Monad[F]) = new IsomorphismMonad[({type λ[α] = LazyEitherT.LeftProjectionT[F, L, α]})#λ, ({type λ[α] = LazyEitherT[F, L, α]})#λ] {
    implicit def G = lazyEitherTMonad[F, L]
    def iso = LazyEitherT.lazyEitherTLeftProjectionEIso2[F, L]
  }
  implicit def lazyEitherTFoldable[F[_], L](implicit F0: Foldable[F]) = new LazyEitherTFoldable[F, L] {
    implicit def F = F0
  }
  implicit def lazyEitherTLeftProjectionFoldable[F[_], L](implicit F0: Foldable[F]) = new IsomorphismFoldable[({type λ[α] = LazyEitherT.LeftProjectionT[F, L, α]})#λ, ({type λ[α] = LazyEitherT[F, L, α]})#λ] {
    implicit def G = lazyEitherTFoldable[F, L]
    def iso = LazyEitherT.lazyEitherTLeftProjectionEIso2[F, L]
  }
}

// TODO more instances
trait LazyEitherTInstances extends LazyEitherTInstances0 {
  implicit def lazyEitherTBiTraverse[F[_]](implicit F0: Traverse[F]) = new LazyEitherTBiTraverse[F] {
    implicit def F = F0
  }
  implicit def lazyEitherTLeftProjectionBiTraverse[F[_]](implicit F0: Traverse[F]) = new IsomorphismBiTraverse[({type λ[α, β] = LazyEitherT.LeftProjectionT[F, α, β]})#λ, ({type λ[α, β] = LazyEitherT[F, α, β]})#λ] {
    implicit def G = lazyEitherTBiTraverse[F]
    def iso = LazyEitherT.lazyEitherTLeftProjectionIso2[F]
  }

  implicit def lazyEitherTTraverse[F[_], L](implicit F0: Traverse[F]) = new LazyEitherTTraverse[F, L] {
    implicit def F = F0
  }

  implicit def lazyEitherTLeftProjectionTraverse[F[_], L](implicit F0: Traverse[F]) = new IsomorphismTraverse[({type λ[α]=LazyEitherT.LeftProjectionT[F, L, α]})#λ, ({type λ[α]=LazyEitherT[F, L, α]})#λ] {
    implicit def G = lazyEitherTTraverse[F, L]
    def iso = LazyEitherT.lazyEitherTLeftProjectionEIso2[F, L]
  }
}

trait LazyEitherTFunctions {
  def lazyEitherT[F[_], A, B](a: F[LazyEither[A, B]]): LazyEitherT[F, A, B] = new LazyEitherT[F, A, B] {
    val run = a
  }

  import LazyEither._

  def lazyLeftT[F[_], A, B](a: => A)(implicit p: Pointed[F]): LazyEitherT[F, A, B] =
    lazyEitherT(p.point(lazyLeft(a)))

  def lazyRightT[F[_], A, B](b: => B)(implicit p: Pointed[F]): LazyEitherT[F, A, B] =
    lazyEitherT(p.point(lazyRight(b)))
  
  import Isomorphism.{IsoFunctorTemplate, IsoBiFunctorTemplate}

  implicit def lazyEitherTLeftProjectionEIso2[F[_], E] = new IsoFunctorTemplate[({type λ[α] = LazyEitherT.LeftProjectionT[F, E, α]})#λ, ({type λ[α] = LazyEitherT[F, E, α]})#λ] {
    def to[A](fa: LazyEitherT.LeftProjectionT[F, E, A]): LazyEitherT[F, E, A] = fa.lazyEitherT
    def from[A](ga: LazyEitherT[F, E, A]): LazyEitherT.LeftProjectionT[F, E, A] = ga.left
  }
  implicit def lazyEitherTLeftProjectionIso2[F[_]] = new IsoBiFunctorTemplate[({type λ[α, β] = LazyEitherT.LeftProjectionT[F, α, β]})#λ, ({type λ[α, β] = LazyEitherT[F, α, β]})#λ] {
    def to[A, B](fa: LazyEitherT.LeftProjectionT[F, A, B]): LazyEitherT[F, A, B] = fa.lazyEitherT
    def from[A, B](ga: LazyEitherT[F, A, B]): LazyEitherT.LeftProjectionT[F, A, B] = ga.left
  }
}

//
// Type class implementation traits
//

trait LazyEitherTFunctor[F[_], E] extends Functor[({type λ[α]=LazyEitherT[F, E, α]})#λ] {
  implicit def F: Functor[F]

  override def map[A, B](fa: LazyEitherT[F, E, A])(f: (A) => B): LazyEitherT[F, E, B] = fa map (a => f(a))
}

trait LazyEitherTPointed[F[_], E] extends Pointed[({type λ[α]=LazyEitherT[F, E, α]})#λ] with LazyEitherTFunctor[F, E] {
  implicit def F: Pointed[F]

  def point[A](a: => A): LazyEitherT[F, E, A] = LazyEitherT.lazyRightT(a)
}

trait LazyEitherTApplicative[F[_], E] extends Applicative[({type λ[α]=LazyEitherT[F, E, α]})#λ] with LazyEitherTPointed[F, E] {
  implicit def F: Applicative[F]

  override def ap[A, B](fa: => LazyEitherT[F, E, A])(f: => LazyEitherT[F, E, (A) => B]): LazyEitherT[F, E, B] = fa ap f
}

trait LazyEitherTMonad[F[_], E] extends Monad[({type λ[α]=LazyEitherT[F, E, α]})#λ] with LazyEitherTApplicative[F, E] {
  implicit def F: Monad[F]

  def bind[A, B](fa: LazyEitherT[F, E, A])(f: (A) => LazyEitherT[F, E, B]): LazyEitherT[F, E, B] = fa flatMap (a => f(a))
}

trait LazyEitherTFoldable[F[_], E] extends Foldable.FromFoldr[({type λ[α]=LazyEitherT[F, E, α]})#λ] {
  implicit def F: Foldable[F]

  def foldRight[A, B](fa: LazyEitherT[F, E, A], z: => B)(f: (A, => B) => B): B = fa.foldRight(z)(f)
}

trait LazyEitherTTraverse[F[_], E] extends Traverse[({type λ[α]=LazyEitherT[F, E, α]})#λ] with LazyEitherTFoldable[F, E] {
  implicit def F: Traverse[F]

  def traverseImpl[G[_]: Applicative, A, B](fa: LazyEitherT[F, E, A])(f: (A) => G[B]): G[LazyEitherT[F, E, B]] = fa traverse f

  override def foldRight[A, B](fa: LazyEitherT[F, E, A], z: => B)(f: (A, => B) => B): B = fa.foldRight(z)(f)
}

trait LazyEitherTBiFunctor[F[_]] extends BiFunctor[({type λ[α, β] = LazyEitherT[F, α, β]})#λ] {
  implicit def F: Functor[F]

  def bimap[A, B, C, D](fab: LazyEitherT[F, A, B])(f: A => C, g: B => D) =
    fab.map(x => g(x)).left.map(x => f(x))
}

trait LazyEitherTBiTraverse[F[_]] extends BiTraverse[({type λ[α, β] = LazyEitherT[F, α, β]})#λ] {
  implicit def F: Traverse[F]

  def bitraverse[G[_]: Applicative, A, B, C, D](fab: LazyEitherT[F, A, B])(f: (A) => G[C], g: (B) => G[D]): G[LazyEitherT[F, C, D]] =
    Applicative[G].map(F.traverse(fab.run)(BiTraverse[LazyEither].bitraverseF(f, g)))(LazyEitherT.lazyEitherT(_))
}