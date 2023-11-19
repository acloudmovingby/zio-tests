import zio.{stream, _}
import zio.stream.{ZSink, ZStream}

import java.io.IOException

object ZStreamTiming {
    val emptyStream: ZStream[Any, Nothing, Nothing] = ZStream.empty
    val oneIntValueStream: ZStream[Any, Nothing, Int] = ZStream.succeed(4)
    val oneListValueStream: ZStream[Any, Nothing, List[Int]] = ZStream.succeed(List(1, 2, 3))
    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, 10)
    val infiniteIntStream: ZStream[Any, Nothing, Int] = ZStream.iterate(1)(_ + 1)
    val scheduledStream: ZStream[Any, Nothing, Long] = ZStream.fromSchedule(Schedule.spaced(1.second) >>> Schedule.recurs(10))
    val scheduledAlphabetStream: ZStream[Any, Nothing, String] = ZStream("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        .schedule(Schedule.spaced(2.second))
    case class Item(id: String, sleepTime: Long)
    val finiteItemStream: ZStream[Any, Nothing, Item] = ZStream(
        Item(1.toString, 5000L), // longest
        Item(2.toString, 0L),
        Item(3.toString, 5L),
        Item(4.toString, 50L),
        Item(5.toString, 500L),
        Item(6.toString, 2000L)
    )

    val theProgram: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- Console.printLine("The Stream:")
            _ <- ZStream.fromIterator(Iterator("A", "B", "C"))
                .tap(Console.printLine(_))
                .rechunk(5)
                .tap(i => Console.printLine(s"$i$i"))
                .tap(i => Console.printLine(s"$i$i$i"))
                .run(ZSink.drain)
        } yield ()
    }

    def run(): Unit = Main.run(
        theProgram
    )
}
