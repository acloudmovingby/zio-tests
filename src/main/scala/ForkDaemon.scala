import zio._

object ForkDaemon {
    // prints A C B
    val theProgram = for {
        _ <- Console.printLine("A")
        _ <- ZIO.sleep(1000.millis)
            .flatMap(_ => Console.printLine("B"))
            .forkDaemon
        _ <- Console.printLine("C")
    } yield ()


    def run(): Unit = Main.run(
        theProgram
    )
}
