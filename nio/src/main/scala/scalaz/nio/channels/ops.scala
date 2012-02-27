package scalaz
package nio
package channels

import java.nio.channels._
import java.nio.ByteBuffer

/** This trait wraps channel ops in the execution context monad. */
trait ChannelOps extends generic.Iteratees {
  
  object channelio {
    def position(c: FileChannel) =
      contexted(c.position())
    def setPosition(c:FileChannel)(pos:Long) = 
      contexted(c.position(pos))
    def readChannel(c: ReadableByteChannel)(buf: ByteBuffer) =
      contexted(c.read(buf))
    def writeChannel(c: WritableByteChannel)(buf: ByteBuffer) =
      contexted(c.write(buf))
  }
}