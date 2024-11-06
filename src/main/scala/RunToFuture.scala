import zio._
import zio.stream.{ZSink, ZStream}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object RunToFuture {

    // main difference with just `run` is E <: Throwable
    def runToFuture[E <: Throwable, A](program: ZIO[Any, E, A]): Unit = Unsafe.unsafe { implicit unsafe =>
        val foo = Runtime.default.unsafe.runToFuture[E, A](program)
    }

    val theProgram: ZIO[Any, Throwable, Int] = for {
        _ <- Console.printLine("Starting the stream!\n")
        num <- ZIO.succeed(4)
    } yield {
        throw new Exception("I blew up")
        num
    }

    def run(): Unit = runToFuture(
        theProgram
    )
}
