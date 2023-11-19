import zio.{stream, _}
import zio.stream.{ZSink, ZStream}

import java.io.IOException

object ZStreamExperiments {
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

        def randomSleepThenPrint(s: String): ZIO[Any, IOException, String] = for {
            rand <- ZIO.succeed(scala.util.Random.nextLong(300))
            _ <- ZIO.sleep(rand.millis)
            _ <- Console.printLine(s + s)
        } yield s

        def sleepThenPrint(str: String, sleepMs: Long): ZIO[Any, IOException, Unit] = for {
            _ <- ZIO.sleep(sleepMs.millis)
            _ <- Console.printLine(str)
        } yield ()

        def concat(str: String, n: Int): String = List.fill(n)(str).mkString

        for {
            _ <- Console.printLine("The Stream:")
            _ <- finiteItemStream
                .tap(i => Console.printLine(i))
                .rechunk(3) // waits for all 3 to be ready?
                .mapZIOPar(3)(z => sleepThenPrint(concat(z.id, 1), z.sleepTime) *> ZIO.succeed(z)) // 1, 2, 3
                .mapZIO { z => if (z.id == "3") ZIO.fail(new Exception("3 is bad")).either else ZIO.succeed(Right(z)) }
                .rechunk(3)
                .mapZIO((z: Either[Exception, Item]) => z match {
                    case Right(item) => sleepThenPrint(concat(item.id, 2), 0L) *> ZIO.succeed(z)
                    case Left(err) => sleepThenPrint(err.toString, 0L) *> ZIO.succeed(z)
                })
                .run(ZSink.drain)
        } yield ()
    }

    val printStream: ZIO[Any, Throwable, Unit] = {
        scheduledAlphabetStream.foreach(i => Console.printLine(i.toString))
    }

    def run(): Unit = Main.run(
        theProgram
        //printStream
    )
}
