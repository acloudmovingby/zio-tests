import zio.{ZIO, _}

object Main extends App {

    def run[E, A](program: ZIO[Any, E, A]): Unit = Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.run[E, A](program) match {
            case Exit.Success(value) => println(s"Success! value=$value")
            case Exit.Failure(cause) => println(s"Failure! cause=$cause")
        }
    }

    Sandbox.run()
    //FailureExperiments.run()
    //ZLayerExperiments.run()
    //ParallelProcessingExperiments.run()
    //ScheduleExperiments.run()
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
    //Broadcast.run()
    //ZStreamTiming.run()
    //ConsoleInputStream.run()
    //ZStreamFailure.run()
    //EnsuringExperiment.run()
    //ScopeExperiment.run()
    //ForkDaemon.run()
    //MinimalScopeExample.run()
    //ZStreamASBSender.run()
    //RunToFuture.run()
    //Foo2.playWithWaitNotify()
    //BufferOnZStream.run()
    //RunningFold.run()
    //StartQueueStreamAsync.run()
    //ZIOClockCommon.run() // can't use because it must be a test
}
import scala.collection.immutable.Queue

class Foo2 {
    private val lock = 1
    private var q: Queue[Int] = Queue.empty

    val enqueuer = new Thread {
        override def run(): Unit = {
            for (i <- 1 to 10) {
                lock.synchronized {
                    println(f"Enqueued $i")
                    q :+= i
                    try { lock.notifyAll() } catch { case e: InterruptedException => println(s"notifyAll failed: $e")}
                }
                Thread.sleep(100)
            }
        }
    }

    val consumer = new Thread {
        override def run(): Unit = {
            while (true) {
                lock.synchronized {
                    if (q.nonEmpty) {
                        val head = q.headOption
                        q = q.tail
                        println(s"head=$head")
                        try { lock.wait() } catch { case e: InterruptedException => println(s"notifyAll failed: $e")}
                    }
                }
            }
        }
    }
}

object Foo2 {
    def playWithWaitNotify(): Unit = {
        val f = new Foo2()

        f.consumer.start()
        f.enqueuer.start()
    }

}