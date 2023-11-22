import zio.{stream, _}
import zio.stream.{ZSink, ZStream}

import java.io.IOException

object ZStreamFailure {

    /**
     * Test the behavior of ZStream when a failures or exceptions are thrown in the middle of processing the stream.
     * Does it continue to process more items? Where does the failure/exception get passed?
     *
     * ANSWER: yes, it stops the whole stream.
     */

    val emptyStream: ZStream[Any, Nothing, Nothing] = ZStream.empty
    val oneIntValueStream: ZStream[Any, Nothing, Int] = ZStream.succeed(4)
    val oneListValueStream: ZStream[Any, Nothing, List[Int]] = ZStream.succeed(List(1, 2, 3))
    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, 5)
    val infiniteIntStream: ZStream[Any, Nothing, Int] = ZStream.iterate(1)(_ + 1)
    val scheduledStream: ZStream[Any, Nothing, Long] = ZStream.fromSchedule(Schedule.spaced(1.second) >>> Schedule.recurs(10))
    val scheduledAlphabetStream: ZStream[Any, Nothing, String] = ZStream("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        .schedule(Schedule.spaced(2.second))

    var index = 0
    val failIndex = 3

    // Here we simply throw an exception. Interestingly, if you delete that first tap(...printLine), it will never print
    // any of the items in the second tap. Another example of: never assume a consistent ordering in how items are processed
    val program1: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- finiteIntStream
                .tap(Console.printLine(_)) // if you delete this
                .map(n => {
                    index = index + 1
                    (n, index)
                })
                .map { case (n, ix) =>
                    if (ix >= failIndex) {
                        throw new Exception
                        n // will never reach
                    } else {
                        n
                    }
                }
                .tap(Console.printLine(_)) // you never actually get to this
                .run(ZSink.drain)
        } yield ()
    }

    // Here I tried using mapZIO and wrapping the exception throwing in ZIO.attempt
    // ... the result is the same, it behaves the same way.
    val program2: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- finiteIntStream
                .tap(Console.printLine(_)) // if you delete this
                .map(n => {
                    index = index + 1
                    (n, index)
                })
                .mapZIO {
                    case (n, ix) => ZIO.attempt {
                        if (ix >= failIndex) {
                            throw new Exception
                            n // will never reach
                        } else {
                            n
                        }
                    }
                }
                .tap(Console.printLine(_)) // you never actually get to this
                .run(ZSink.drain)
        } yield ()
    }

    // Here I tried using .retry(Schedule.exponential(...)) to see how we could recover from the stream failure
    // It's, uh, interesting... not what I would've expected. It basically starts the whole stream over again from the beginning
    // how does this work with an infinite stream??? Is it keeping all of initial elements in memory forever?? (or maybe just the chunk?)
    val retryFailIndex = 3
    val withRetry: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- finiteIntStream
                .tap(x => Console.printLine(s"A$x"))
                .map(n => {
                    index = index + 1
                    (n, index)
                })
                .mapZIO {
                    case (n, ix) => ZIO.attempt {
                        if (ix >= retryFailIndex) {
                            throw new Exception
                            n // will never reach
                        } else {
                            n
                        }
                    }
                }
                .tap(x => Console.printLine(s"BB$x"))
                .retry(Schedule.exponential(10.millis))
                .run(ZSink.drain)
        } yield ()
    }

    def run(): Unit = Main.run(
        //program1
        program2
        //withRetry
    )
}
