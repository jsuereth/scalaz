package scalaz
package nio

/** Default "library" for Iteratees. */
object std extends generic.Iteratees
           with    generic.IterateeUtils
           with    channels.ByteChannels
           with    channels.FileChannels
           with    channels.ChannelUtils 
           with    channels.CharChannels {
  type Context[X] = effect.IO[X]
  override implicit def context_monad = effect.IO.ioMonad
}