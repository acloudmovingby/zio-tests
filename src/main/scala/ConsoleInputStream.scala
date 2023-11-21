import zio.{stream, _}
import zio.Console.printLine
import zio.stream.{ZSink, ZStream}
import zio.Clock

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import Utils._

object ConsoleInputStream {
    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, 10)

    def doSomethingWithConsoleInput[E, A](f: String => ZIO[Any, E, A]) =
        Main.run(Console.readLine.flatMap(f).repeatWhile(_ => true))

    class Thing {
        def registerCallback(onEvent: String => Unit): Unit = {
            val t = new Thread(() => {
                for (i <- 1 to 10) {
                    Thread.sleep(1000)
                    onEvent(s"event $i")
                }
            })
            t.start()
        }
    }

    val t = new Thing()

    val stream = ZStream.async[Any, Nothing, String] { cb =>
        t.registerCallback(s => cb(ZIO.succeed(Chunk(s))))
    }

    val program: ZIO[Any, Any, Unit] = {
        for {
            //_ <- doSomethingWithConsoleInput(s => printWithTime(s"input = $s"))
            _ <- stream
                .tap(s => printWithTime(s"input = $s"))
                .run(ZSink.drain)
//            _ <- finiteIntStream
//                .mapChunksZIO(c => printWithTime(s"chunk size = ${c.length}").map(_ => c))
//                .tap(n => printWithTime("(STEP 1) " + n))
//                .buffer(16) // doesn't force it to rechunk to 16, only if consumer is slower than producer
//                .tap(n => sleepAndZIO(printWithTime("(STEP 2)  " + n)))
//                .foreach(n => printWithTime("(STEP 3)   " + n))
        } yield ()
    }

    def run(): Unit = Main.run(
        program
    )
}
