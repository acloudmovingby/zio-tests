import zio._
import zio.stream.{ZPipeline, ZSink, ZStream}

import java.io.IOException

object ZStreamASBSender {

    val numInts = 27
    val parallelism = 2
    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, numInts)

    class Batch {

        val id: Int = scala.util.Random.nextInt(1000)

        val maxBatchSize = 3
        var counter = 0

        def add(id: Int): Boolean = {
            counter += 1
            counter < maxBatchSize
        }
        def send(): Unit = Thread.sleep(100)
    }

    var counter = 0

    val schedge = Schedule.fixed(5.seconds).jittered

    val theProgram: ZIO[Any, Throwable, Unit] = for {
        _ <- Console.printLine("Starting the stream!\n")
        startTime <- Clock.nanoTime
        foo <- finiteIntStream
            .aggregateAsync(ZSink.foldWeighted[Int, Long](0)((acc: Long, i: Int) => acc, 10) { (acc, i) =>
                    println(s"i=$i, acc=$acc")
                    acc + i
            })
            .tap(i => Console.printLine(s"result=$i"))
            .run(ZSink.drain)
        foo <- finiteIntStream
            .mapAccum("")((s: String, i: Int) => (s + i.toString, s + i.toString))
            .tap(i => Console.printLine(s"result=$i"))
            .run(ZSink.drain)
        _ <- Console.printLine(s"foo = $foo")
        endTime <- Clock.nanoTime
        _ <- Console.printLine(s"Elapsed time = ${(endTime - startTime).floatValue() / 1000000000F}, non-parallel, would've taken ${numInts} seconds")
    } yield ()

    def run(): Unit = Main.run(
        theProgram
    )
}
