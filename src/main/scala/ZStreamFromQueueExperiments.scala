import zio._
import zio.stream.{ZSink, ZStream}

import java.io.IOException

object ZStreamFromQueueExperiments {

    def startQueue: ZIO[Any, IOException, Queue[Int]] = for {
        _ <- Console.printLine("Starting Queue...")
        q <- Queue.bounded[Int](100)
        _ <- ZStream.fromQueue(q)
            .mapZIOPar(100) {i =>
                ZIO.sleep(scala.util.Random.nextLong(1000).millis) *>
                Console.printLine(s"From queue: ${i.toString}")
            }
            .run(ZSink.drain)
    } yield q

    val theProgram = for {
        _ <- Console.printLine("Starting theProgram")
        q <- startQueue
        items = Range.inclusive(1, 10).toList
        _ <- Console.printLine(s"Before offering items")
        _ <- q.offerAll(items)
        _ <- Console.printLine(s"After offering items")
    } yield ()


    def run(): Unit = Main.run(
        theProgram
    )
}
