import java.io.IOException
import zio._
import zio.stream.{ZSink, ZStream}

object Sandbox {

    var counter = 0
    var isRunning = true

    def dbOperation(): Unit = {
        counter += 1
        if (counter > 3) isRunning = false
        println(s"DB operation $counter")
    }



    val dbOperationZIO: ZIO[Any, Throwable, Unit] = ZIO.attempt(dbOperation())

    val dbOperationStream: ZStream[Any, Throwable, Unit] =
        ZStream.fromZIO(dbOperationZIO)

    def runStream(z: ZStream[Any, Throwable, Unit]): ZIO[Any, Throwable, Unit] = {
        z
            .repeat(Schedule.forever)
            .tap { _ =>
                Console.printLine(s"tapping repeated stream: $counter")
            }
            .takeWhile(_ => isRunning)
            .run(ZSink.drain)
    }

    def resetCounters(): Unit = {
        counter = 0
        isRunning = true
    }

    val theProgram: ZIO[Any, Throwable, Unit] = for {
        _ <- Console.printLine("The Stream:")
        _ <- ZStream(dbOperation _) // Method 1: Use a function reference
            .map(foo => foo())
            .tap(_ => Console.printLine(s"tap: $counter"))
            .repeat(Schedule.forever)
            .takeWhile(_ => counter < 5 && isRunning)
            .tap(_ => Console.printLine(s"tap: $counter"))
            .run(ZSink.drain)
        _ = resetCounters()
        _ <- ZStream.fromZIO(dbOperationZIO) // Method 2: Use fromZIO
            .tap(_ => Console.printLine(s"tap: $counter"))
            .repeat(Schedule.forever)
            .takeWhile(_ => counter < 5 && isRunning)
            .tap(_ => Console.printLine(s"tap: $counter"))
            .run(ZSink.drain)
        _ = resetCounters()
        _ <- runStream(dbOperationStream) // ...
    } yield ()

    def run(): Unit = Main.run(
        theProgram
    )
}
