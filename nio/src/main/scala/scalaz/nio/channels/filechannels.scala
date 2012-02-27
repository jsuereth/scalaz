package scalaz
package nio
package channels

import syntax.monad._
import scalaz.nio.buffers.{ImmutableBuffer, Read}
import java.nio.channels.FileChannel

trait FileChannels extends generic.Iteratees with ChannelOps {
  import iteratees._
  
  object filechannels {
    type RByteBuffer = ImmutableBuffer[Byte,Read]
    /** Marker trait for RandomAccess consumer "error channel" messages. */
    trait RandomAccessError extends ProcessingError
    object RandomAccessError {
      /** An error state that denotes the Producing file channel should
       *  seek to the given offset, and then being pushing to the desired iteratee.
       *  
       *  TODO - Find  a way to bubble up nested stream-command Iteratees for
       *  wrapping/conversion etc.
       */
      case class SeekTo[I,O](offset: Long, next: Consumer[I,O]) extends RandomAccessError
    }
    
    /** An Iteratee that initiates a "seek to" message to a file channel.
     * 
     *  TODO - make this work with map/flatMap/zip.
     */
    def seekTo(offset: Long): Consumer[RByteBuffer, Unit] =
      Consumer.error(RandomAccessError.SeekTo(offset, Consumer.done[RByteBuffer, Unit]((), EOF)), EOF)

      
    /** A Producer that will read the bytes in a file channel, and allow
     * the iteratees to go into a "seek" error, where the producer will
     * move the channel to a new position and then continue feeding
     * data.
     */
    def read_file_bytes(channel: FileChannel): Producer[RByteBuffer] =
      new Producer[RByteBuffer] {
        override def into[O](c: Consumer[RByteBuffer,O]): Consumer[RByteBuffer,O] = {
          def drive(channel: FileChannel, c: Consumer[RByteBuffer,O]): Consumer[RByteBuffer,O] = 
            Consumer flatten c.fold {
              case c @ Consumer.Done(_,_) => contexted(Consumer(c))
              case Consumer.Error(RandomAccessError.SeekTo(offset, next), _) =>
                for {
                  pos <- channelio.position(channel)
                  _   <- channelio.setPosition(channel)(pos + offset)
                } yield drive(channel, next.asInstanceOf[Consumer[RByteBuffer, O]])
              case c @ Consumer.Error(_,_) => contexted(Consumer(c))
              case Consumer.Processing(f) =>
                for {
                  buf <- contexted(java.nio.ByteBuffer.allocate(64*1024))
                  r <- channelio.readChannel(channel)(buf)
                  next = r match {
                    case -1 => EOF
                    case _  => Chunk(ImmutableBuffer.fromJavaBytesWriting(buf,copy=false).flip)
                  }
                } yield drive(channel, f(next))
            }
          drive(channel, c)
        }
      }
  }
}