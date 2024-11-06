import Utils._
import java.util.concurrent.TimeUnit
import zio._
import zio.stream.{ZSink, ZStream}

object BufferOnZStream {

    val programFoo: ZIO[Any, Any, Unit] = {
        for {
            startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
            _ <- ZStream
                .fromIterable(1 to 100)
                .rechunk(10)
                .tap { x =>
                    Clock.sleep(100.milliseconds) *>
                    Console.printLine(s"before buffering: $x")
                }
                .groupedWithin(10, 1.seconds)
                //.rechunk(1)
                .buffer(2)
                .tap { x =>
                        Console.printLine(s"--after buffering: $x")
                }
                .tap { x =>
                    Clock.sleep(2.seconds) *>
                    Console.printLine(s"----double after buffering: $x")
                }
                .run(ZSink.drain)
            elapsedTime <- Clock.currentTime(TimeUnit.MILLISECONDS).map(_ - startTime)
            _ <- Console.printLine(s"Elapsed time: $elapsedTime")
        } yield ()
    }

    def run(): Unit = Main.run(
        programFoo
    )
}

case class Foo(x: Int)
