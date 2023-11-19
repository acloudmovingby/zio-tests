import zio.{stream, _}
import zio.Console.printLine
import zio.stream.{ZSink, ZStream}

import java.io.IOException

object ConsoleInputStream {
    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, 100)

    val program: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- finiteIntStream
                .foreach(Console.printLine(_))
        } yield ()
    }

    def run(): Unit = Main.run(
        program
    )
}
