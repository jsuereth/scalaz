package scalaz
package example


object NioUsage {
  import nio.std._
  import effect.IO
  import syntax.monad._
  import nio.buffers._

  /** Ensures a file is closed. */
  def withFile[A](file: java.io.File)(proc: java.nio.channels.FileChannel => IO[A]): IO[A] =
    for {
      stream <- IO(new java.io.FileInputStream(file))
      result <- proc(stream.getChannel) onException(IO(stream.close()))
      _      <- IO(stream.close())
    } yield result


  def sha1sumLzy(file: java.io.File): IO[String] =
    withFile(file) { c =>
      val bytes = bytechannels read_channel_bytes c
      bytes into bytechannelutils.sha1string result
    }
  def sha1sum(file: java.io.File) = sha1sumLzy(file).unsafePerformIO

  // Mimics WC command-line
  def wcLzy(file: java.io.File): IO[String] =
    withFile(file) { c =>
      val bytes = bytechannels.read_channel_bytes(c, directBuffers=true)
      val wordCount = charchannels.words convert utils.counter
      val lineCount = charchannels.lines convert utils.counter
      val allCount = lineCount zip wordCount zip utils.lengthCounter(_.remaining) map {
         case((lc, wc), cc) => "lines: %d, words %d, chars %s" format (lc,wc,cc)
      } 
      (bytes convert charsets.decoder() 
             into allCount 
             result)
    }
  def wc(file: java.io.File): String = wcLzy(file).unsafePerformIO
}


