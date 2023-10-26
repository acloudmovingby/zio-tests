import zio.{stream, _}
import zio.stream.{ZSink, ZStream}

import java.io.IOException
import java.util.concurrent.TimeUnit
import java.lang.System
import java.math.BigInteger

object GroupByKey {
    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, 100)

    val program: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- ZIO.unit
            _ <- finiteIntStream
                .groupByKey(i => i % 10){
                    case (key, subStream) => ZStream.fromZIO(subStream.runCollect.map(l => key -> l.size))
                }
                .tap(i => ZIO.attempt(println(i)))
                .run(ZSink.drain)
        } yield ()
    }
    def run(): Unit = Main.run(
        program
    )
}
