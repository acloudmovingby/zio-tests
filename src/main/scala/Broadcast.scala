import zio.{stream, _}
import zio.stream.{ZSink, ZStream}

import java.io.IOException
import java.util.concurrent.TimeUnit
import java.lang.System
import java.math.BigInteger

object Broadcast {
    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, 100)

    val program: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- ZIO.unit
            _ <- ZIO.scoped {
                finiteIntStream
                    .broadcast(2, 5)
                    .tap(i => ZIO.attempt(println(i)))
            }
        } yield ()
    }

    def run(): Unit = Main.run(
        program
    )
}
