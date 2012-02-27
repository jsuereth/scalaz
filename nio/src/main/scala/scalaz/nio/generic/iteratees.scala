package scalaz
package nio
package generic

/** A Module of sorts to contain generic implementation of Iteratees. */
trait Iteratees {
  /** The Monad through which iteratees are threaded. */
  type Context[X]  
  implicit def context_monad: Monad[Context]  
  /** Converts a raw value into a Context wrapped value */
  def contexted[A](a: A): Context[A] = context_monad.point(a)
  /** "package" containing iteratee module. */
  object iteratees {
    /** Base class trait for Error-channel between producers/consumers */
    trait ProcessingError
    /** A fatal processing error that may have occurred. */
    case class FatalProcessingError(t: Throwable) extends ProcessingError
    
    
    
    /** The lowest trait that represents values that can be passed to consumers.  This hierarchy is
     * expected to be extended for 'smart' communication in a domain.   For example: Network
     * Iteratees may be able to pass new types of stream values whereas File Iteratees might not.
     */
    sealed trait StreamValue[+T]
    /** A value that represents the end of the input. */
    case object EOF extends StreamValue[Nothing]
    /** A value that represents a new chunk of input. */
    case class Chunk[Input](input: Input) extends StreamValue[Input]
    case object EmptyChunk extends StreamValue[Nothing]
    /** Marker trait to extract the existing state of a consumer. */
    sealed trait ConsumerState[I,O]
    
    /** This represents an immutable stream consumer that will eventually produce a value.
     * A stream Consumer can be in one of two states:
     * 
     * 1. Finished with a value
     * 2. Waiting for more input.
     */
    trait Consumer[I,O] {
  
      /** Fold accepts three continuations for the three states of this consumer.
       *  The consumer will take whichever action is appropriate given its state.    
       */
      def fold[R](f: ConsumerState[I,O] => Context[R]): Context[R]
      
      /** Attempts to pull a 'finished' value out of the Consumer by passing an
       * EOF token to it.
       * 
       * Note: This could throw an error if the Consumer is not finished consuming.
       */
      def result: Context[O] = fold {
        case Consumer.Done(o, i)    => contexted(o)
        case Consumer.Error(e, _)   => sys.error("Processing Erorr: " + e)
        case Consumer.Processing(f) => 
          f(EOF) fold {
            case Consumer.Done(o,_)     => contexted(o)
            case Consumer.Processing(_) => sys.error("Divergent Consumer")
            case Consumer.Error(e,_)    => sys.error("Processing Erorr: " + e)
          }
      }
      
      /**
       * Flattens this Consumer if it's the result of a Converter application or
       * returns another consumer for some reason.
       */
      def join[I2, O2](implicit ev0: O <:< Consumer[I2, O2]): Consumer[I,O2] =
        this flatMap { iter2 => 
          Consumer flatten iter2.fold[Consumer[I,O2]] {
            case Consumer.Done(a, i)    => contexted(Consumer.done(a, EmptyChunk))
            case Consumer.Error(e,_)    => contexted(Consumer.error(e, EmptyChunk))
            case Consumer.Processing(k) => k(EOF) fold {
              case Consumer.Done(a, i)    => contexted(Consumer.done(a, EmptyChunk))
              case Consumer.Error(e, _)   => contexted(Consumer.error(e, EmptyChunk))
              case Consumer.Processing(_) => sys.error("join: Divergent iteratee!")
            }
          }
        }    
      // Note: These are here to defeat Scala's implicit system...
      def map[B](f: O => B): Consumer[I,B] =
        std.consumerInstance.map(this)(f)
      def flatten[B](implicit ev0: O <:< Consumer[I,B]): Consumer[I,B] =
        std.consumerInstance.bind(this)(ev0)  
      def flatMap[B](f: O => Consumer[I,B]): Consumer[I,B] =
        std.consumerInstance.bind(this)(f)
      /** Co-process two Consumers with the same input. */  
      def zip[B](c: Consumer[I, B]): Consumer[I, (O,B)] = new ZippedConsumer(this, c)
    }
    
    /** A class which drives two consumers with the same input until both are done. */
    private[this] class ZippedConsumer[I,A,B](first: Consumer[I,A], second: Consumer[I,B]) extends Consumer[I, (A,B)] {
      def fold[R](f: ConsumerState[I,(A,B)] => Context[R]): Context[R] =
        first.fold { state1 =>
          second.fold { state2 =>
            (state1, state2) match {
              case (Consumer.Done(a, i), Consumer.Done(b, _)) => f(Consumer.Done(a->b, i))
              case (Consumer.Error(e,k), _)                   => f(Consumer.Error(e,k))
              case (_, Consumer.Error(e,k))                   => f(Consumer.Error(e,k))
              case _                                          => f(Consumer.Processing(drive))
            }
          }
        }
      // Helper method to push data through both iteratees.
      private def drive(input: StreamValue[I]): Consumer[I, (A,B)] =
        new ZippedConsumer(Producer.once(input) into first, Producer.once(input) into second)
      
    }
    
    object Consumer {
      /** The 'done' state of a consumer.  The result is available for usage. */
      case class Done[I,O](result: O, lastInput: StreamValue[I]) extends ConsumerState[I,O]
      /** The 'processing' state of a consumer.  Contains the function to run on the next set of input. */
      case class Processing[I,O](consumeNext: StreamValue[I] => Consumer[I,O]) extends ConsumerState[I,O]
      /** The 'error' state of a consumer.  Contains the error meessage and the last input encountered. 
       * 
       *  NOTE: RandomAcess iteratees *FORCE* use to keep some way of regenerating an Consumer if the Producer
       *  is able to "recover" from the error (i.e. handle the error-channel messaging).   This means we
       *  need a mechanism of propogating a (onFix: => Consumer[I,O) parameter here.
       */
      case class Error[I,O](error: ProcessingError, lastInput: StreamValue[I]) extends ConsumerState[I, O]
      
      /** Constructs a new consumer in the done state. */
      def done[I,O](value: O, remaining: StreamValue[I]) =
        Consumer(Done[I,O](value, remaining))
      /** Constructs a consumer that accepts the next stream input and
       * returns the next consumer.
       */
      def cont[I,O](f: StreamValue[I] => Consumer[I,O]) =
        Consumer(Processing(f))
      /** Constructs a consumer that represents an error condition. */
      def error[I,O](err: ProcessingError, last: StreamValue[I]) =
        Consumer[I,O](Error[I,O](err, last))
      // TODO - Better implementation of this.
      def apply[I,O](state: ConsumerState[I,O]): Consumer[I,O] = 
        new Consumer[I,O] {
          override def fold[R](f: ConsumerState[I,O] => Context[R]): Context[R] = f(state)
          override def toString = "Consumer("+state+")"
        }
      /** Removes the Context from around a consumer.
       * TODO - Look into making this implicit...
       */
      def flatten[I,O](i: Context[Consumer[I,O]]) =
        new Consumer[I,O] {
          override def fold[R](f: ConsumerState[I,O] => Context[R]): Context[R] = 
            context_monad.bind(i)(_.fold(f))
          override def toString = "Flattened("+i+")"
        }  
    }
    
    /**
     * This class produces values and feeds them through a Consumer.  It returns the
     * immutable consumer reuslting from feeding the value.
     */
    trait Producer[A] { self =>
      /** Feeds values from this producer into a consumer. */
      def into[O](c: Consumer[A, O]): Consumer[A, O]
      /** Append a second producer's input after this one. */
      def andThen(p: Producer[A]) = new Producer[A] {
        override def into[O](c: Consumer[A,O]): Consumer[A,O] =
          p into (self into c)
        override def toString = "Producer("+self+" andThen "+p+")"
      }
      /** Transforms this Producer by a stream conversion. */
      def convert[A2](conversion: StreamConversion[A, A2]): Producer[A2] =
        new Producer[A2] {
          override def into[O](c: Consumer[A2,O]): Consumer[A2,O] =
            Consumer flatten (self into (conversion apply c)).result
          override def toString = "ConvertedProducer("+self+" by " + conversion +")"
        }
    }
    
    object Producer {
      /** Constructs a producer the feeds one value into a consumer and then stops. */
      def once[T](in: StreamValue[T]): Producer[T] = new Producer[T] {
        override def into[O](c: Consumer[T, O]): Consumer[T, O] = 
          Consumer flatten (c fold {
            case Consumer.Done(_,_)     => contexted(c)
            case Consumer.Processing(f) => contexted(f(in))
            case Consumer.Error(_, _)   => contexted(c)
          })
        override def toString = "Producer.once("+in+")"
      }
      /** Sends the EOF input to a Consumer. */
      def eof[T] = once[T](EOF: StreamValue[T])
    }
    
    /**
     * This class converts a consumer of one input type to a consumer of another input type.  These
     * usually represent things like decrypted input streams, conversion from Byte to Char or
     * grouping input for efficient consumption.
     */
    trait StreamConversion[I,I2] {
      /** Converts a Consumer of a given input type to a consumer of a new input type that returns
       * a completed consumer of the old input type.
       */
      def apply[O](i: Consumer[I2,O]): Consumer[I, Consumer[I2, O]]
      /** Convers a Consumer of one input type into a consumer of another input type. */
      def convert[O](i: Consumer[I2, O]): Consumer[I, O] =
        apply(i).join
    }
    
    object StreamConversion {
      /** Constructs a StreamConversion from a raw function on input types. */
      def make[A,B](f: A => B): StreamConversion[A,B] = new StreamConversion[A,B] {
        def apply[O](i: Consumer[B,O]): Consumer[A, Consumer[B,O]] = {
          def wrapInput(in: StreamValue[A]): StreamValue[B] = in match {
            case EmptyChunk => EmptyChunk
            case EOF => EOF
            case Chunk(i) => Chunk(f(i))
          }
          def next(wrapped: Consumer[B,O]): StreamValue[A] => Consumer[A,Consumer[B,O]] = { in =>
            Consumer.flatten(wrapped fold {
              case Consumer.Processing(f) => contexted(Consumer.cont(next(f(wrapInput(in)))))
              case x                      => contexted(Consumer.done(Consumer(x), in))
            })
          }
          Consumer.cont(next(i))
        } 
      }
    }
    
    
    object std {
      implicit def consumerInstance[I] = new Monad[({type C[A] = Consumer[I,A]})#C] { instance =>
        import Consumer._
        // Applicative-style invocation should "zip" iteratees together (i.e. parallel consumption).
        override def ap[A,B](fa: => Consumer[I, A])(f: => Consumer[I, A => B]): Consumer[I, B] = 
          fa zip f map { case (a, f) => f(a) }
        override def point[A](a: => A): Consumer[I,A] =
          Consumer.done(a, EmptyChunk)
        override def map[A,B](ma: Consumer[I, A])(f: A=>B): Consumer[I, B] = new Consumer[I, B] {
          // TODO - memoize the result of a fold, if possible.
          override def fold[R](f2: ConsumerState[I,B] => Context[R]): Context[R] =
            ma fold {
              case Done(a, i)     => f2(Done(f(a), i))
              case Processing(f3) => f2(Processing(f3 andThen (instance.map(_)(f))))
              case Error(e,i)     => f2(Error(e,i))
            }
        }
        override def bind[A,B](ma: Consumer[I,A])(f: A => Consumer[I,B]): Consumer[I,B] = 
          Consumer flatten ma.fold[Consumer[I,B]] {
            case Done(a, next)  => contexted(Producer once next into f(a))
            case Processing(k)  => contexted(cont(in => bind(k(in))(f)))
            case Error(e, next) => contexted(error(e,next))
          }
      }
    }
  }  
}