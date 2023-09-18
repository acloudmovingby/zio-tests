import zio._

import java.io.IOException
import java.util.concurrent.TimeUnit

object ConcurrencyExperiments {
    // have two processes happening concurrently and prove that that is true
    // one process that prints after 1 second, one after 2 seconds, then record time at end, it should be less than 3 seconds?

    def sleepThenPrint(str: String, sleepMs: Long): ZIO[Any, IOException, Unit] = for {
        _ <- ZIO.sleep(sleepMs.millis)
        _ <- Console.printLine(str)
    } yield ()

    val printConcurrently = for {
        _ <- Console.printLine("Starting printConcurrently")
        begin <- Clock.currentTime(TimeUnit.MILLISECONDS)
        a <- sleepThenPrint("A", 2000L).fork
        b <- sleepThenPrint("B", 1000L).fork
        aJoined <- a.join
        bJoined <- b.join
        end <- Clock.currentTime(TimeUnit.MILLISECONDS)
        _ <- Console.printLine(s"Ending printConcurrently. Time elapsed=${end - begin}")
    } yield ()

    def run(): Unit = Main.run(
        printConcurrently
    )
}
