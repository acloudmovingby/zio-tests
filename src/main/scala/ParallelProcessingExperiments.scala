import java.util.concurrent.TimeUnit
import zio._
import zio.stream.{ZSink, ZStream}

object ParallelProcessingExperiments {
    def processQueueInParallel(
        queue: Queue[Int],
        process: Int => String,
        numParallelProcessors: Int
    ): ZStream[Any, Throwable, (Int, String)] =
        ZStream.fromQueue(queue)
            .tap(x => Clock.currentTime(TimeUnit.MILLISECONDS).map(y => println(s"x=$x, curTime=$y")))
            .mapZIOPar(numParallelProcessors)(msg => ZIO.attempt((msg, process(msg))))
            .tap(x => Clock.currentTime(TimeUnit.MILLISECONDS).map(y => println(s"x=$x, curTime=$y")))

    val sleepTime = 200
    val numItems = 5
    val numParallelProcessors = 10 // doesn't matter, just make more than numItems

    val processingFunction = (_: Int) => {
        Thread.sleep(sleepTime)
        "string"
    }

    val whatToRun = for {
        queue <- Queue.bounded[Int](10)
        items = List.range(0, numItems)
        _ <- queue.offerAll(items)
        shutdown <- ZIO.sleep((100).millis)
            .flatMap(_ => queue.shutdown)
            .fork
        startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
        _ = println("start time: " + startTime)
        _ <- processQueueInParallel(queue, processingFunction, numParallelProcessors)
            .run(ZSink.drain)
        endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
        _ = println("end time: " + endTime)
        _ <- shutdown.join
    } yield endTime - startTime

    def run(): Unit = Main.run(whatToRun)
}
