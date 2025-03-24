import java.io.IOException
import zio._
import zio.stream.{ZSink, ZStream}

object Sandbox {

    var counter = 0

    def dbOperation() = {
        println(s"DB operation $counter")
        counter += 1
    }

    def getFromDB() = ZIO.succeed(dbOperation())

    val theProgram: ZIO[Any, Throwable, Unit] = for {
        _ <- ZStream(getFromDB())
            .takeWhile(_ => counter < 5)
            .run(ZSink.drain)
    } yield ()

    def run(): Unit = Main.run(
        theProgram
    )
}
