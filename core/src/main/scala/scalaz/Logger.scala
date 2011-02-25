package scalaz

object LoggerContainer {
// todo not List -- just using it now for simplification
  type C[A] = List[A]
}

import LoggerContainer._

sealed trait Logger[M[_], Z, A] extends NewType[WriterT[M, C[Z], A]] {
  val logger: WriterT[M, C[Z], A]
}

object Logger {
  implicit def LoggerPure[M[_]: Pure, Z]: Pure[({type λ[α]= Logger[M, Z, α]})#λ] = new Pure[({type λ[α]=Logger[M, Z, α]})#λ] {
    def pure[A](a: => A) =
      error("")
  }

  implicit def LoggerFunctor[M[_]: Functor, Z]: Functor[({type λ[α]=Logger[M, Z, α]})#λ] = new Functor[({type λ[α]= Logger[M, Z, α]})#λ] {
    def fmap[A, B](x: Logger[M, Z, A], f: A => B) =
      error("")
  }

  implicit def LoggerApply[M[_], Z](implicit ma: Apply[M], ftr: Functor[M]): Apply[({type λ[α]=Logger[M, Z, α]})#λ] = new Apply[({type λ[α]=Logger[M, Z, α]})#λ] {
    def apply[A, B](f: Logger[M, Z, A => B], a: Logger[M, Z, A]): Logger[M, Z, B] =
      error("")
  }

  implicit def LoggerBind[M[_], Z](implicit mnd: Monad[M]): Bind[({type λ[α]=Logger[M, Z, α]})#λ] = new Bind[({type λ[α]=Logger[M, Z, α]})#λ] {
    def bind[A, B](a: Logger[M, Z, A], f: A => Logger[M, Z, B]) = error("")
  }

  implicit def LoggerEach[M[_]: Each, Z]: Each[({type λ[α]=WriterT[M, Z, α]})#λ] = new Each[({type λ[α]= WriterT[M, Z, α]})#λ] {
    def each[A](x: WriterT[M, Z, A], f: A => Unit) =
      error("")
  }
}

trait Loggers {
  def logger[M[_], Z, A](w: WriterT[M, C[Z], A]): Logger[M, Z, A] = new Logger[M, Z, A] {
    val value = w
    val logger = w
  }
}
