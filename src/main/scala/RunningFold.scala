import zio._
import zio.stream.{ZSink, ZStream}

import java.io.IOException

object RunningFold {

    val infiniteRandomInts: ZStream[Any, Nothing, Int] =
        ZStream.repeatZIO(Random.nextIntBounded(10))

    def repeatFoldLeft[E, A, B](
        stream: ZStream[Any, E, A],
        z: => B,
        op: (B, A) => B,
        p: (B, A) => Boolean, // predicate to say when to stop current fold and begin a new one with z
        bufferSize: Int = 16,
        maxTime: Duration = Duration.fromMillis(100)
    ): ZIO[Any, Nothing, ZStream[Any, E, B]] = {
        var accumulator = z
        for {
            q <- Queue.bounded[B](bufferSize)
            _ <- stream
                .mapZIO { i =>
                    if (p(accumulator, i)) {
                        q.offer(accumulator).map(_ => accumulator = z)
                    } else {
                        ZIO.attempt { accumulator = op(accumulator, i) }
                    }
                }
                .run(ZSink.drain)
                .fork
        } yield ZStream.fromQueue(q)
    }

    val program: ZIO[Any, Any, Unit] = for {
        infinite <- ZIO.succeed(infiniteRandomInts.tap(i => Console.printLine(s"i=$i")))
        transformed <- repeatFoldLeft[IOException, Int, Int](
            infinite,
            0,
            (s: Int, i: Int) => i + s,
            (s: Int, _: Int) => s > 10
        )
        _ <- transformed
            .take(2)
            .map(s => println(s"minisum=$s\n"))
            .run(ZSink.drain)
    } yield ()

    def run(): Unit = Main.run(
        program
    )
}
