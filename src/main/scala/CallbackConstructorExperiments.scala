import zio._

// Didn't finish this experiment as i found online that it may be impossible to do without passing the Runtime into the callback
case class Subscriber(callback: String => Unit) {
    def triggerCallback(s: String): Unit = callback(s)
}

object CallbackConstructorExperiments {
    val theProgram: ZIO[Any, Throwable, Unit] = for {
        _ <- Console.printLine("Starting theProgram")
        _ <- Console.printLine("Ending theProgram")
    } yield ()

    def run(): Unit = Main.run(
        theProgram
    )
}
