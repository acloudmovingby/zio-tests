import zio.{stream, _}
import zio.stream.{ZSink, ZStream}

import java.io.IOException

object ZStreamFailure {

    /**
     * Test the behavior of ZStream when a failures or exceptions are thrown in the middle of processing the stream.
     * Does it continue to process more items? Where does the failure/exception get passed?
     */

    val emptyStream: ZStream[Any, Nothing, Nothing] = ZStream.empty
    val oneIntValueStream: ZStream[Any, Nothing, Int] = ZStream.succeed(4)
    val oneListValueStream: ZStream[Any, Nothing, List[Int]] = ZStream.succeed(List(1, 2, 3))
    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, 10)
    val infiniteIntStream: ZStream[Any, Nothing, Int] = ZStream.iterate(1)(_ + 1)
    val scheduledStream: ZStream[Any, Nothing, Long] = ZStream.fromSchedule(Schedule.spaced(1.second) >>> Schedule.recurs(10))
    val scheduledAlphabetStream: ZStream[Any, Nothing, String] = ZStream("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        .schedule(Schedule.spaced(2.second))



    val theProgram: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- Console.printLine("The Stream:")
            _ <- finiteIntStream
                .tap(Console.printLine(_))
                .run(ZSink.drain)
        } yield ()
    }

    def run(): Unit = Main.run(
        theProgram
    )
}
