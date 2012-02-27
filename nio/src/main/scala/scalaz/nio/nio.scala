package scalaz
package nio

/** Default "library" for Iteratees. */
object std extends generic.Iteratees
           with    generic.IterateeUtils
           with    channels.ByteChannels
           with    channels.ChannelUtils {
  type Context[X] = effect.IO[X]
  override implicit def context_monad = effect.IO.ioMonad
}