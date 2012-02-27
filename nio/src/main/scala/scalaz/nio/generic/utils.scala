package scalaz
package nio
package generic

/** Generic utility Iteratees usable regardless of incoming inputs. */
trait IterateeUtils extends Iteratees {
  import iteratees._

  object utils {
    /**
     * Consumes the head element from the stream and places it
     * back onto the stream.
     */
    def peek[I]: Consumer[I, Option[I]] = Consumer cont {
      case EmptyChunk    => peek[I]
      case c @ Chunk(in) => Consumer.done(Some(in), c)
      case e @ EOF       => Consumer.done(None, e)
    }
    /**
     * Consumes the head element off the incoming stream.
     */
    def head[I]: Consumer[I, Option[I]] = Consumer cont {
      case EmptyChunk => head[I]
      case c @ Chunk(in) => Consumer.done(Some(in), EmptyChunk)
      case e @ EOF => Consumer.done(None, e)
    }
    
    def counter[I]: Consumer[I, Long] = {
      def step(count: Long): StreamValue[I] => Consumer[I,Long] = {
        case e @ EOF      => Consumer.done(count, e)
        case c @ Chunk(_) => Consumer cont step(count + 1)
        case EmptyChunk   => Consumer cont step(count)
      }
      Consumer cont step(0)
    }
    
    def lengthCounter[I](length: I => Long) = {
      def step(count: Long): StreamValue[I] => Consumer[I,Long] = {
        case e @ EOF      => Consumer.done(count, e)
        case Chunk(i)     => Consumer cont step(count + length(i))
        case EmptyChunk   => Consumer cont step(count)
      }
      Consumer cont step(0)
    }
    
    /** Repeats a consumer indefinitely until end of file, storing results in a Vector. */
    def repeat[I,O](in: Consumer[I, O]): Consumer[I, Vector[O]] = {
      def step(current: Vector[O]): StreamValue[I] => Consumer[I,Vector[O]] = {
        case e @ EOF      => Consumer.done(current, e)
        case c @ Chunk(_) =>
          Consumer flatten in.fold[Consumer[I,Vector[O]]] {
              case Consumer.Done(value, el) => contexted(Consumer.done(current :+ value, el))
              case Consumer.Error(err, el)  => contexted(Consumer.error(err, el))
              case Consumer.Processing(k)   => contexted(for(h <- k(c); t <- repeat(in)) yield h +: t)
          }
      }
      Consumer cont step(Vector.empty)
    }  

  }
}