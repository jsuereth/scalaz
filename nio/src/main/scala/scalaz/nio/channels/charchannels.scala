package scalaz
package nio
package channels

import syntax.monad._
import java.nio.charset.{CoderResult,Charset,CharsetDecoder}

import scalaz.nio.buffers.{ImmutableBuffer,Read}

trait CharChannels extends generic.Iteratees {
  import iteratees._
  
  /** "package" with charset encoding/decoding stream converters. */
  object charsets {
    type RByteBuffer = ImmutableBuffer[Byte,Read]
    type RCharBuffer = ImmutableBuffer[Char, Read]
    /** Decodes a stream of bytebuffers into a stream of character buffers.
     * The conversion from byte -> char is done using the passed in charset.
     * 
     * Note: The charset decoders used are from Java, and therefore stateful.  While
     * this class attempts to remain as composable as possible, it's best to avoid
     * using this class in any operation that must backtrack.
     */
    def decoder(charset: Charset = Charset.defaultCharset): StreamConversion[RByteBuffer, RCharBuffer] =
      new StreamConversion[RByteBuffer, RCharBuffer] {
        def apply[O](i: Consumer[RCharBuffer,O]): Consumer[RByteBuffer, Consumer[RCharBuffer, O]] = {
          def next(decoder: CharsetDecoder, nested: Consumer[RCharBuffer, O])(input: StreamValue[RByteBuffer]): Consumer[RByteBuffer, Consumer[RCharBuffer, O]] =
            input match {
              case e @ EOF => Consumer.done(Producer.eof into nested, e)
              case c @ Chunk(s) =>
                // Ok, now we need to read this into our decoder...
                def read(nested: Consumer[RCharBuffer,O]): Consumer[RByteBuffer, Consumer[RCharBuffer, O]] = {
                  val nextBuf = java.nio.CharBuffer.allocate(s.remaining)
                  decoder.decode(s.toJavaByteBuffer, nextBuf, false) match {
                    case CoderResult.UNDERFLOW => 
                      // TODO - Use immutable char-buffer
                      nextBuf.flip()
                      val nextNested = Producer.once(Chunk(ImmutableBuffer.fromJavaCharsReading(nextBuf))) into nested
                      Consumer.cont(next(decoder, nextNested))
                    case CoderResult.OVERFLOW =>
                      // TODO - Use immutable char-buffer
                      nextBuf.flip()
                      val nextNested = Producer.once(Chunk(ImmutableBuffer.fromJavaCharsReading(nextBuf))) into nested
                      read(nextNested)
                    case x => 
                      // TODO - Better error case.
                      Consumer.error(FatalProcessingError(new Exception("Bad character encoding: " + x)), c)
                  }
                }
                read(nested)
            }
          Consumer.cont(next(charset.newDecoder, i))
        }
      }
    
  }
  
  /** "Package" for utilities that deal with character channels. */
  object charchannels {
    type RByteBuffer = ImmutableBuffer[Byte,Read]
    type RCharBuffer = ImmutableBuffer[Char, Read]
    
    /** Splits a incoming charbuffer stream by whitespace and returns all non-empty words. */
    def words: StreamConversion[RCharBuffer, String] = new StreamConversion[RCharBuffer,String] {
      def apply[O](i: Consumer[String, O]): Consumer[RCharBuffer, Consumer[String,O]] = {
        // TODO - use a rope or something better
        def next(prev: String, nested: Consumer[String,O])(input: StreamValue[RCharBuffer]): Consumer[RCharBuffer, Consumer[String,O]] =
          input match {
            case e @ EOF => Consumer.done(Producer.eof into nested, e)
            case Chunk(cbuf) =>
              def readChar(buf: RCharBuffer, nested: Consumer[String,O], sb: StringBuffer): Consumer[String,O] =
                if (!buf.hasRemaining) nested
                else buf.next match {
                  case (c, nextbuf) if c.isWhitespace  =>
                    val input = sb.toString
                    val newnested = if(input.isEmpty) nested 
                                    else Producer.once(Chunk(sb.toString)) into nested
                    readChar(nextbuf, newnested, new StringBuffer)
                  case (c, nextbuf)                    => 
                    sb append c
                    readChar(nextbuf, nested, sb)
                }
              val nexti = readChar(cbuf, nested, new StringBuffer(prev))
              Consumer.cont(next("", nexti))
          }
        Consumer.cont(next("", i))
      }
    }
    /** Splits an incoming character stream by line endings ('\r', '\n' or '\r\n'). */
    def lines: StreamConversion[RCharBuffer, String] = new StreamConversion[RCharBuffer,String] {
      def apply[O](i: Consumer[String, O]): Consumer[RCharBuffer, Consumer[String,O]] = {
        // TODO - use a rope or something better
        def next(prev: String, nested: Consumer[String,O])(input: StreamValue[RCharBuffer]): Consumer[RCharBuffer, Consumer[String,O]] =
          input match {
            case e @ EOF => Consumer.done(Producer.eof into nested, e)
            case Chunk(cbuf) =>
              def readChar(buf: RCharBuffer, skipLF: Boolean, nested: Consumer[String,O], sb: StringBuffer): Consumer[String,O] =
                if (!buf.hasRemaining) nested
                else buf.next match {
                  case ('\n', nextbuf) if skipLF       => 
                    readChar(nextbuf, false, nested, sb)
                  case (c @ ('\n' | '\r'), nextbuf)    =>
                    val newnested = Producer.once(Chunk(sb.toString)) into nested
                    readChar(nextbuf, c == '\r', newnested, new StringBuffer)
                  case (x, nextbuf)                    => 
                    sb append x
                    readChar(nextbuf, false, nested, sb)
                }
              val nexti = readChar(cbuf, false, nested, new StringBuffer(prev))
              Consumer.cont(next("", nexti))
          }
        Consumer.cont(next("", i))
      }
    }
  }
}