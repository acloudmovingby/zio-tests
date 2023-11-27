import zio._

object FailureExperiments {
    val attemptThrow = ZIO.attempt(throw new Exception("Throw within attempt!"))
    val succeedThrow = ZIO.succeed(throw new Exception("Throw within succeed!"))
    val theProgram = for {
        _ <- Console.printLine("Starting program...")
        _ <- attemptThrow.tapError(e => ZIO.attempt(println(s"attemptThrow e=$e")))
        _ <- succeedThrow.onError(e => ZIO.succeed(println(s"succeedThrow e=$e")))
//        _ = throw new Exception("Just throw!") // Exit.Failure ,but is a Die
//        _ <- ZIO.succeed(throw new Exception("Throw within succeed!")) // same as above
//        _ <- ZIO.attempt(throw new Exception("Throw within attempt!")) // Still Exit.Failure, but is a Fail not a Die
        _ <- ZIO.attempt(throw new Exception("hey")) *> ZIO.succeed(println("jude"))
    } yield ()

    def run(): Unit = Main.run(
        theProgram
    )
}
