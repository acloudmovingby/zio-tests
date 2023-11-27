import java.io.IOException
import scala.io.Source
import zio._
import zio.stream.ZSink

object ScopeExperiment {

    def acquire(name: => String): ZIO[Any, IOException, Source] =
        ZIO.attemptBlockingIO(Source.fromFile(name))

    def release(source: => Source): ZIO[Any, Nothing, Unit] =
        ZIO.succeedBlocking(source.close())

    def source(name: => String): ZIO[Scope, IOException, Source] =
        ZIO.acquireRelease(acquire(name))(release(_))

    val scopeReadFile: ZIO[Any, Throwable, Unit] =
        for {
            lines <- ZIO.scoped {
                for {
                    source <- source("/Users/coates/Documents/code/zio-tests/src/main/scala/ScopeExperiment.scala")
                    lines <- ZIO.attemptBlockingIO(source.getLines().mkString("\n")) // if you move this out of the ZIO.scoped, it will error saying source is already closed
                } yield lines
            }
            _ <- Console.printLine(lines)
        } yield ()

    // make a Queue scoped so we are sure it will be shut down in the end
    def acquireQueue(queueSize: => Int): UIO[Queue[Int]] = Queue.bounded[Int](queueSize)

    def releaseQueue(queue: => Queue[Int]): ZIO[Any, Nothing, Unit] = for {
        _ <- ZIO.succeed(println("Shutting down queue"))
        _ <- queue.shutdown
        _ = println("Shutting down queue again")
        _ <- queue.shutdown
        _ = println("Finished shutting down queue")
    } yield ()

    def sourceQueue(queueSize: => Int): ZIO[Scope, Nothing, Queue[Int]] =
        ZIO.acquireRelease(acquireQueue(queueSize))(releaseQueue(_))

    val scopeQueue: ZIO[Any, Throwable, Unit] =
        for {
            queue <- ZIO.scoped {
                for {
                    queue <- sourceQueue(10)
                    _ <- queue.offer(1)
                } yield queue
            }
            _ <- Clock.sleep(500.millis)
            i <- queue.take // blows up...
            _ <- Console.printLine(s"i=$i")
        } yield ()

    def run(): Unit = Main.run(
       //scopeReadFile
       scopeQueue
    )
}
