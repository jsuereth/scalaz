package scalaz

import std.AllInstances._
import scalaz.scalacheck.ScalazProperties._
import scalaz.scalacheck.ScalazArbitrary._

class StreamTTest extends Spec {
  type StreamTOpt[A] = StreamT[Option, A]

  "fromStream / toStream" ! check {
    (ass: Stream[Stream[Int]]) =>
      StreamT.fromStream(ass).toStream must be_===(ass)
  }

  "filter all" ! check {
    (ass: StreamT[Stream, Int]) =>
      ass.filter(_ => true) must be_===(ass)
  }

  "filter none" ! check {
    (ass: StreamT[Stream, Int]) =>
      val filtered = ass.filter(_ => false)
      val isEmpty = filtered.isEmpty
      !isEmpty.contains(true)
  }
  
  "drop" ! check {
    (ass: Option[Stream[Int]], x: Int) =>
      StreamT.fromStream(ass).drop(x).toStream must be_===(ass.map(_.drop(x)))
  }
  
  "take" ! check {
    (ass: Option[Stream[Int]], x: Int) =>
      StreamT.fromStream(ass).take(x).toStream must be_===(ass.map(_.take(x)))
  }

  checkAll(equal.laws[StreamTOpt[Int]])
  checkAll(monoid.laws[StreamTOpt[Int]])
  checkAll(monad.laws[StreamTOpt])
  
  object instances {
    def semigroup[F[_]: Functor, A] = Semigroup[StreamT[F, A]]
    def monoid[F[_]: Pointed, A] = Monoid[StreamT[F, A]]
    def functor[F[_]: Functor, A] = Functor[({type λ[α]=StreamT[F, α]})#λ]
    def monad[F[_]: Monad, A] = Monad[({type λ[α]=StreamT[F, α]})#λ]
  }
}
