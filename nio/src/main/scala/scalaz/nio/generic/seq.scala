package scalaz
package nio
package generic


object seq extends generic.Iteratees
           with    generic.IterateeUtils {
  import iteratees._
  type Context[X] = Free.Trampoline[X]
  override implicit def context_monad = Free.trampolineMonad
  
  def enum[T](t: TraversableOnce[T]): Producer[T] =
    new Producer[T] {
      override def into[O](c: Consumer[T,O]): Consumer[T,O] = 
        // TODO - use breakable and fold on consumer for
        // early break in iteration.
        t.foldLeft(c) {
          case (c, value) =>
            Producer.once(Chunk(value)) into c
        }
      
    }
}