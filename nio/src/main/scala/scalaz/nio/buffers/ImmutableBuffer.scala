package scalaz
package nio
package buffers

/** A phantom parent type used to determine if a byte buffer is readable or writeable. */
sealed trait ReadWrite {
  /** Returns the inverse type for this value.  If read, then write, etc. */
  type Reverse <: ReadWrite
}
/** A phantom type representing if an immutable byte buffer can be read from. */
class Read extends ReadWrite {
  type Reverse = Write
}
/** A phantom type representing if an immutable byte buffer can be written to. */
class Write extends ReadWrite {
  type Reverse = Read
}
object ReadWrite {
  /** This trait represents something that can flip an immutable byte buffer from read to write. */
  trait Flipper[T <: ReadWrite] {
    /** Flips an immutable byte buffer from reading to writing. Copies if needed. */
    def flip[A: ClassManifest](b: ImmutableBuffer[A,T]): ImmutableBuffer[A, T#Reverse]
  }
  implicit object ReadFlipper extends Flipper[Read] {
    /** Flips a readable buffer into a writable byte buffer.   Must copy the bytes
     * so that things can be written.
     */
    override def flip[A: ClassManifest](b: ImmutableBuffer[A,Read]): ImmutableBuffer[A, Write] = {
      val newBuf = new Array[A](b.buffer.size)
      // TODO - Do we need to copy the whole thing?
      System.arraycopy(b.buffer, 0, newBuf, 0, newBuf.size)
      new ImmutableBuffer(newBuf,0,b.position)
    }
  }
  /** A flipper for writeable byte buffers. */
  implicit object WriteFlipper extends Flipper[Write] {
    /** Flips a writeable byte buffer to a readable byte buffer.
     *  Does not need to copy the bytes.
     */
    override def flip[A: ClassManifest](b: ImmutableBuffer[A, Write]): ImmutableBuffer[A, Read] =
      new ImmutableBuffer(b.buffer,0,b.position)
  }
}

object ImmutableBuffer {
  /** Constructs a new ImmutableByteBuffer for reading */
  def reading[T: ClassManifest](size: Int)(position: Int = 0, limit: Int = size) = 
    new ImmutableBuffer[T, Read](new Array[T](size), position, limit)
  /** Constructs a new ImmutableByteBuffer for writing */
  def writing[T: ClassManifest](size: Int)(position: Int = 0, limit: Int = size) =
    new ImmutableBuffer[T, Write](new Array[T](size), position, limit)
  /** Constructs a new ImmutableByteBuffer based on a java.nio.ByteBuffer */
  def fromJavaBytes[RW <: ReadWrite](b: java.nio.ByteBuffer, copy: Boolean = false) = {
    if (b.hasArray) {
      // TODO - Copy the array?
      val array = if(copy) b.array.clone() else b.array()
      new ImmutableBuffer[Byte, RW](array, b.position, b.limit)
    } else sys.error("Unsupported operation: Converting non-array-backed java.nio.ByteBuffer to ImmutableByteBuffer")
  }
  /** Converts a `java.nio.ByteBuffer` into a readable `ImmutableByteBuffer`. */
  def fromJavaBytesReading(b: java.nio.ByteBuffer, copy: Boolean = false) =
    fromJavaBytes[Read](b, copy)
  /** Converts a `java.nio.ByteBuffer` into a writeable `ImmutableByteBuffer`. */
  def fromJavaBytesWriting(b: java.nio.ByteBuffer, copy: Boolean = false) =
    fromJavaBytes[Write](b, copy)
  /** Constructs a new ImmutableBuffer based on a java.nio.CharBuffer */
  def fromJavaChars[RW <: ReadWrite](b: java.nio.CharBuffer, copy: Boolean = false) = {
    if (b.hasArray) {
      // TODO - Copy the array?
      val array = if(copy) b.array.clone() else b.array()
      new ImmutableBuffer[Char, RW](array, b.position, b.limit)
    } else sys.error("Unsupported operation: Converting non-array-backed java.nio.ByteBuffer to ImmutableByteBuffer")
  }
  /** Converts a `java.nio.CharBuffer` into a readable `ImmutableCharBuffer`. */
  def fromJavaCharsReading(b: java.nio.CharBuffer, copy: Boolean = false) =
    fromJavaChars[Read](b, copy)
  /** Converts a `java.nio.CharBuffer` into a writeable `ImmutableCharBuffer`. */
  def fromJavaCharsWriting(b: java.nio.CharBuffer, copy: Boolean = false) =
    fromJavaChars[Write](b, copy)
}

/**
 * This class represents a readable/writable buffer of bytes.  The
 * value of the buffer is immutable and does not change over time.
 * It is backed by an Array and attempts to efficiently share data across instances.
 */
class ImmutableBuffer[@specialized(Byte,Char) T : ClassManifest, RW <: ReadWrite](
    private[nio] val buffer: Array[T],
    val position: Int,
    val limit: Int) {
  /** The amount of space remaining in the buffer.  If reading from the buffer,
   * this indicates the amount of data left to be read.  If writing, this represents
   * the remaining capacity inside the buffer.
   */
  def remaining = limit - position  
  
  def hasRemaining: Boolean = remaining > 0
  /** Converts this buffer from reading to writing and vice versa.
   * Note:  This method may need to make a copy of the buffer before
   * returning a new immutable instance.
   */
  def flip(implicit f: ReadWrite.Flipper[RW]) = f.flip(this)
  /**
   * Creates a new 'clear' buffer the same size as the previous buffer.
   */
  def clear: ImmutableBuffer[T, RW] =
    new ImmutableBuffer[T, RW](new Array[T](buffer.size), 0, buffer.size)
  
  /** Copies this byte buffer with modified values. */
  private[nio] def copy(buffer: Array[T] = this.buffer, 
           position: Int = this.position,
           limit: Int = this.limit) =
    new ImmutableBuffer[T, RW](buffer, position, limit)
  
  // ------------------------- Read Methods -------------------------------
  
  /** Reads the next byte available in this buffer, if the buffer is a read buffer. */
  def next(implicit ev: RW <:< Read): (T, ImmutableBuffer[T, RW]) = {
    assert(remaining > 0)
    (buffer(position), this.copy(position = position+1))
  }
  /** Reads the next bytes available in this buffer, if the buffer is a write buffer */
  def nextArray(dst: Array[T])( 
                offset: Int = 0, 
                length: Int = dst.size)(
                 implicit ev: RW <:< Read): ImmutableBuffer[T, RW] = {
    assert(remaining >= length)
    System.arraycopy(buffer, position, dst, offset, length)
    this.copy(position = position + length)
  }
  /** When reading the byte buffer, this method will rewing to the beginning of
   * the data stream.
   * 
   * TODO - Write version of this method.
   */
  def rewind(implicit ev: RW <:< Read): ImmutableBuffer[T, RW] =
    new ImmutableBuffer[T, RW](buffer, 0, limit)
  /**
   * When reading the byte buffer, this method will migrate the remaining
   * values to the beginning of the byte buffer.
   * 
   * TODO - Write version of this method.
   */
  def compact(implicit $ev0: RW <:< Read): ImmutableBuffer[T, Read] = {
    // TODO - Better copy
    val next = new Array[T](buffer.size)
    System.arraycopy(buffer, position, next, 0, limit)
    new ImmutableBuffer[T, Read](buffer, 0, limit)    
  }
  
  // ------------------------- Write Methods ------------------------------
  
  /** Writes a single byte into a writeable byte buffer.   Returns the new ByteBuffer
   * instance.
   */
  def write(byte: T)(implicit ev: RW <:< Write): ImmutableBuffer[T, RW] = {
    assert(remaining > 0)
    buffer(position) = byte
    this.copy(position = position + 1)
  }
  /** Writes a sequence of bytes into the byte buffer.   Returns the modified
   * byte buffer instance.
   */
  def writeArray(src: Array[T])(
                 offset: Int = 0,
                 length: Int = src.size)(
                 implicit ev: RW <:< Write): ImmutableBuffer[T, RW] = {
    assert(remaining >= length)
    System.arraycopy(src, offset, buffer, position, length)
    this.copy(position = position + length)
  }
  
  def toJavaByteBuffer(implicit ev: T =:= Byte) = {
    val buf = java.nio.ByteBuffer.wrap(buffer.asInstanceOf[Array[Byte]])
    buf.position(position)
    buf.limit(limit)
    buf
  }
  
  def toJavaCharBuffer(implicit ev: T =:= Char) = {
    val buf = java.nio.CharBuffer.wrap(buffer.asInstanceOf[Array[Char]])
    buf.position(position)
    buf.limit(limit)
    buf
  }
  
  override def toString = "Immutable"+implicitly[ClassManifest[T]]+"Buffer(buffer="+buffer+",position="+position+",limit="+limit+")"
}

