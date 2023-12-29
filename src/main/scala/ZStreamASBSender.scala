import zio._
import zio.stream.{ZSink, ZStream}

import java.io.IOException

object ZStreamASBSender {
    val emptyStream: ZStream[Any, Nothing, Nothing] = ZStream.empty
    val oneIntValueStream: ZStream[Any, Nothing, Int] = ZStream.succeed(4)
    val oneListValueStream: ZStream[Any, Nothing, List[Int]] = ZStream.succeed(List(1, 2, 3))
    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, 10)
    val infiniteIntStream: ZStream[Any, Nothing, Int] = ZStream.iterate(1)(_ + 1)
    val scheduledStream: ZStream[Any, Nothing, Long] = ZStream.fromSchedule(Schedule.spaced(1.second) >>> Schedule.recurs(10))
    val scheduledAlphabetStream: ZStream[Any, Nothing, String] = ZStream("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        .schedule(Schedule.spaced(2.second))

    val theProgram: ZIO[Any, Throwable, Unit] = for {
        _ <- Console.printLine("Starting the stream!")
        _ <- finiteIntStream
            .mapZIO(i => Console.printLine(i.toString))
            .run(ZSink.drain)

    } yield ()

    def run(): Unit = Main.run(
        theProgram
    )
}
