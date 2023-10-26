import zio._
import zio.stream.{ZSink, ZStream}

object MapChunkExperiments {

    // Goal: to collect a group of elements from a ZStream and perform an operation on them in batches (chunks)

    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, 100)

    val chunkSize = 10

    val theProgram = for {
        _ <- finiteIntStream
            .tap(i => {
                val sleepTime = scala.util.Random.nextLong(1000)
                ZIO.sleep(sleepTime.millis) *>
                Console.printLine(s"i=$i, time=$sleepTime")
            })

            // (1) `rechunk` blocks until a full chunk is ready
            /*
            .rechunk(chunkSize)
            .mapChunks(chunk => { // note must use mapChunks
                println(chunk.toString)
                chunk
            })
            */

            // (2) `groupedWithin` blocks until chunk full OR time limit reached, whichever comes first
            .groupedWithin(chunkSize, 1.second) // blocks until chunk full OR
            .map(chunk => {
                println(chunk.toString)
            })

            .run(ZSink.drain)
        _ <- Console.printLine("Ending theProgram")
    } yield ()

    def run(): Unit = Main.run(
        theProgram
    )
}
