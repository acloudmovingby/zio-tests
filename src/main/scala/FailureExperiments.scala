import zio._

object FailureExperiments {
    val theProgram = for {
//        _ = throw new Exception("Just throw!") // Exit.Failure ,but is a Die
//        _ <- ZIO.succeed(throw new Exception("Throw within succeed!")) // same as above
//        _ <- ZIO.attempt(throw new Exception("Throw within attempt!")) // Still Exit.Failure, but is a Fail not a Die
        _ <- ZIO.succeed(println("hey")) *> ZIO.succeed(println("jude"))
    } yield ()

    def run(): Unit = Main.run(
        theProgram
    )
}
