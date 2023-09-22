import zio._
import zio.stream.{ZSink, ZStream}

object ZStreamFromQueueExperiments {

    val theProgram = for {
        _ <- Console.printLine("Starting theProgram")
        q <- Queue.bounded[Int](100)
        items = Range.inclusive(1, 100).toList
        offer <- q.offerAll(items).fork
        _ <- ZStream.fromQueue(q)
            .mapZIOPar(100) {i =>
                ZIO.sleep(scala.util.Random.nextLong(1000).millis) *>
                Console.printLine(i.toString)
            }
            .run(ZSink.drain)
        _ <- offer.join
    } yield ()


    def run(): Unit = Main.run(
        theProgram
    )
}
