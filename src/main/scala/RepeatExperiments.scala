import zio._

object RepeatExperiments {

    var flag1 = false
    var flag2 = false

    val shutdownQueue = for {
        q <- Queue.bounded[Int](100)
        _ <- q.offerAll(List.range(0, 100))
        _ <- ZIO.sleep(2000.millis)
            .map(_ => flag1 = true)
            .fork
        _ <- ZIO.sleep(100.millis)
            .repeatUntil(_ => flag1)
            .flatMap(_ => q.shutdown)
    } yield ()

    val simpleShutdownQueue = for {
        q <- Queue.bounded[Int](100)
        _ <- q.offerAll(List.range(0, 3))
        shutdown <- q.shutdown.fork
        _ <- Console.printLine("Shutdown called")
        _ <- q.offer(3)
    } yield ()

    val theProgram = for {
        _ <- ZIO.sleep(2000.millis)
            .map(_ => flag1 = true)
            .fork
        _ <- ZIO.sleep(4000.millis)
            .map(_ => flag2 = true)
            .fork
        _ <- (for {
            _ <- ZIO.sleep(100.millis)
                .flatMap(_ => Console.printLine("A"))
                .repeatUntil(_ => flag1)
            _ <- ZIO.sleep(100.millis)
                .flatMap(_ => Console.printLine("B"))
                .repeatUntil(_ => flag2)
        } yield ()).fork
    } yield ()

    def run(): Unit = Main.run(
        simpleShutdownQueue
    )
}
