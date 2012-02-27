package scalaz
package nio
package channels

import java.security.MessageDigest
import scalaz.nio.buffers.{ImmutableBuffer, Read}

/** Generic utility Iteratees usable regardless of incoming inputs. */
trait ChannelUtils extends generic.Iteratees {
  import iteratees._
  
  object channelutils {
    type RByteBuffer = ImmutableBuffer[Byte, Read]
    /** Calculates the digest of a readable byte buffer stream. */
    def digest(md: => MessageDigest): Consumer[RByteBuffer, Array[Byte]] = {
      import Consumer._
      def next(md: MessageDigest)(input: StreamValue[RByteBuffer]): Consumer[RByteBuffer, Array[Byte]] = 
        input match {
          case e @ EOF       => done(md.digest, e)
          case c @ Chunk(in) => md.update(in.toJavaByteBuffer); cont(next(md.clone.asInstanceOf[MessageDigest]))
        }
      cont(next(md))
    }
    /** Simple, dumb method to convert the Array of Bytes into a SHA hex value. */
    private[this] def shaByteArrayToHexString(data: Array[Byte]): String = {
      val buf = new StringBuffer
      for (i <- 0 until data.length) { 
        var halfbyte = (data(i) >>> 4) & 0x0F;
        var two_halfs = 0;
        while(two_halfs < 2) { 
          if ((0 <= halfbyte) && (halfbyte <= 9)) 
            buf.append(('0' + halfbyte).toChar)
          else 
            buf.append(('a' + (halfbyte - 10)).toChar);
          halfbyte = data(i) & 0x0F;
          two_halfs += 1
        }
      } 
      return buf.toString
    }
    // Hashing algorithms
    def sha1 = digest(MessageDigest.getInstance("SHA1"))
    def sha256 = digest(MessageDigest.getInstance("SHA-256"))
    def sha384 = digest(MessageDigest.getInstance("SHA-384"))
    def sha512 = digest(MessageDigest.getInstance("SHA-512"))
    def md2 = digest(MessageDigest.getInstance("MD2"))
    def md5 = digest(MessageDigest.getInstance("MD5"))
    
    // Human readable string hashes, also used in .md5/.sha1 files.
    def sha1string = sha1 map shaByteArrayToHexString
    def sha256string = sha256 map shaByteArrayToHexString
    def sha384string = sha384 map shaByteArrayToHexString
    def sha512string = sha512 map shaByteArrayToHexString
    def md2string = md2 map shaByteArrayToHexString
    def md5string = md5 map shaByteArrayToHexString
  }
}
