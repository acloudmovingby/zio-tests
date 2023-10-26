import zio.{ZIO, _}

object Main extends App {

    def run[E, A](program: ZIO[Any, E, A]): Unit = Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.run[E, A](program) match {
            case Exit.Success(value) => println(s"Success! value=$value")
            case Exit.Failure(cause) => println(s"Failure! cause=$cause")
        }
    }

    //FailureExperiments.run()
    //ZLayerExperiments.run()
    //ParallelProcessingExperiments.run()
    //ZStreamExperiments.run()
    //QueueExperiments.run()
    //ConcurrencyExperiments.run()
    //ZStreamFromQueueExperiments.run()
    //RepeatExperiments.run()
    //ParallelTimingExperiments.run()
    //SequenceTraverseExperiments.run()
    //MapChunkExperiments.run()
    //IntermittentLoggingZStream.run()
    //GroupByKey.run()
    Broadcast.run()
}