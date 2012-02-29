package scalaz
package nio
package channels

import syntax.monad._

import scalaz.nio.buffers.{ImmutableBuffer,Read}
import java.nio.channels.ByteChannel

trait ByteChannels extends generic.Iteratees with ChannelOps {
  import iteratees._
  
  object bytechannels {
    type RByteBuffer = ImmutableBuffer[Byte,Read]
      
    /** A Producer that will read the bytes in a file channel, and allow
     * the iteratees to go into a "seek" error, where the producer will
     * move the channel to a new position and then continue feeding
     * data.
     */
    def read_channel_bytes(channel: ByteChannel): Producer[RByteBuffer] =
      new Producer[RByteBuffer] {
        override def into[O](c: Consumer[RByteBuffer,O]): Consumer[RByteBuffer,O] = {
          def drive(channel: ByteChannel, c: Consumer[RByteBuffer,O]): Consumer[RByteBuffer,O] = 
            Consumer flatten c.fold {
              case c @ Consumer.Done(_,_) => contexted(Consumer(c))
              case c @ Consumer.Error(_,_) => contexted(Consumer(c))
              case Consumer.Processing(f, _) =>
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
    
    def write_channel_bytes(channel: ByteChannel): Consumer[RByteBuffer, Unit] = {
      object write extends Function1[StreamValue[RByteBuffer], Consumer[RByteBuffer, Unit]] {
        override def apply(in: StreamValue[RByteBuffer]): Consumer[RByteBuffer, Unit] = in match {
          case EmptyChunk => Consumer cont write
          case EOF        => Consumer.done((), in)
          case Chunk(buf: RByteBuffer) => Consumer flatten (
            channelio.writeChannel(channel)(buf.toJavaByteBuffer) map (_ => Consumer cont write)    
          )
        }
      }
      Consumer cont write
    }
  }
}