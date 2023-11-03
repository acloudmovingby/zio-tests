import zio.{stream, _}
import zio.Console.printLine
import zio.stream.{ZSink, ZStream}

import java.io.IOException

object Broadcast {
    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, 100)

    val exampleFromDocs: ZIO[Any, IOException, Unit] =
        ZIO.scoped {
            ZStream
                .fromIterable(1 to 20)
                .mapZIO(_ => Random.nextInt)
                .map(Math.abs)
                .map(_ % 100)
                .tap(e => printLine(s"Emit $e element before broadcasting"))
                .broadcast(2, 5)
                .flatMap { streams =>
                    for {
                        out1 <- streams(0).runFold(0)((acc, e) => Math.max(acc, e))
                            .flatMap(x => printLine(s"Maximum: $x"))
                            .fork
                        out2 <- streams(1).schedule(Schedule.spaced(1.second))
                            .foreach(x => printLine(s"Logging to the Console: $x"))
                            .fork
                        _ <- out1.join.zipPar(out2.join)
                    } yield ()
                }
        }

    val broadcast: ZIO[Any, IOException, Unit] =
        ZIO.scoped {
            ZStream
                .fromIterable(1 to 10)
                //.tap(i => printLine(s"i=$i"))
                .mapChunks { c => println(s"size=${c.size}"); c }
                .broadcast(2, 1)
                .flatMap { streams =>
                    for {
//                        out1 <- streams(0).runFold(0)((acc, e) => Math.max(acc, e))
//                            .flatMap(x => printLine(s"Maximum: $x"))
//                            .fork
                        out1 <- streams(0).schedule(Schedule.spaced(10.millis))
                            .foreach(x => printLine(s"A$x"))
                            .fork
                        out2 <- streams(1).schedule(Schedule.spaced(1.second))
                            .foreach(x => printLine(s"B$x"))
                            .fork
                        //_ <- streams(1).run(ZSink.drain)
                        _ <- out1.join
                        _ <- out2.join
                    } yield ()
                }
        }

    val program: ZIO[Any, Throwable, Unit] = {
        for {
            //_ <- exampleFromDocs
            _ <- broadcast
        } yield ()
    }

    def run(): Unit = Main.run(
        program
    )
}
