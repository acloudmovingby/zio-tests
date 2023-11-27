import zio.{stream, _}
import zio.stream.{ZSink, ZStream}

import java.io.IOException

object EnsuringExperiment {

    // read input from Console and try to make integers of it. If a non-integer is entered, blow up but make sure it does the finalizers

    val finalizer: UIO[Unit] = // in ensuring(finalizer), finalizer can't fail (must be UIO)
        ZIO.succeed(println("Finalizing!"))
    val finalizer2: UIO[Unit] = // in ensuring(finalizer), finalizer can't fail (must be UIO)
        ZIO.succeed(println("Finalizing...a second time!"))

    val theProgram: ZIO[Any, Throwable, Unit] = for {
        _ <- Utils.streamFromConsoleInput()
            .map(input => Integer.valueOf(input))
            .mapZIO(i => Console.printLine(s"input was the number $i"))
            .run(ZSink.drain)
            .onError(cause => ZIO.succeed(println(s"onError logging: ${cause.squash}")))
            .ensuring(finalizer)
    } yield ()

    def run(): Unit = Main.run(
        theProgram.ensuring(finalizer2)
    )
}
