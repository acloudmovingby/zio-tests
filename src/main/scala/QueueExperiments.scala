import zio._

object QueueExperiments {
    val q: UIO[Queue[RuntimeFlags]] = Queue.bounded[Int](100)

    val theProgram: ZIO[Any, Throwable, Unit] = {

    }

    def run(): Unit = Main.run(
        theProgram
    )
}
