import ZLayerExperiments.DatabaseLive
import zio._

object QueueExperiments {

    val q: UIO[Queue[Int]] = Queue.bounded[Int](5)

    val queueLayer: ZLayer[Any, Throwable, Queue[Int]] = ZLayer {
        Console.printLine("Creating queue") *> q
    }

    val offer1: ZIO[Queue[Int], Throwable, Boolean] = for {
        _ <- Console.printLine("Offering 1")
        queue <- ZIO.service[Queue[Int]]
        result <- queue.offer(1)
    } yield result

    val offer2: ZIO[Queue[Int], Throwable, Boolean] = for {
        _ <- Console.printLine("Offering 2")
        queue <- ZIO.service[Queue[Int]]
        result <- queue.offer(2)
    } yield result

    val takeAndPrint: ZIO[Queue[Int], Throwable, Unit] = for {
            queue <- ZIO.service[Queue[Int]]
            _ <- queue.take.flatMap(i => Console.printLine(i.toString))
            _ <- queue.take.flatMap(i => Console.printLine(i.toString))
        } yield ()

    val readInputOfferToQueue: ZIO[Queue[Int], Throwable, Unit] = for {
        queue <- ZIO.service[Queue[Int]]
        _ <- Console.printLine("Enter a number")
        input <- Console.readLine
        _ <- input match {
            case "stop" => for {
                size <- queue.size
                _ <- Console.printLine(s"Queue size=$size. Shutting down...")
                _ <- ZIO.sleep(100.millis).repeatUntilZIO(_ => queue.isEmpty).flatMap(_ => queue.shutdown)
            } yield ()
            case _ => for {
                parsed <- ZIO.attempt(input.toInt)
                _ <- queue.offer(parsed)
                size <- queue.size
                _ <- Console.printLine(s"Queue size=$size")
            } yield ()
        }
    } yield ()

    val takeFromQueue: ZIO[Queue[Int], Throwable, Unit] = for {
        queue <- ZIO.service[Queue[Int]]
        _ <- queue.take.flatMap(i => Console.printLine(s"TAKING FROM QUEUE: ${i.toString}"))
    } yield ()

    val theProgram: ZIO[Queue[Int], Throwable, Unit] = {
        for {
            offer <- readInputOfferToQueue.repeat(Schedule.forever) // asks for number on console, adds to queue
                .fork
            take <- takeFromQueue
                .repeat(Schedule.spaced(200.millis) >>> Schedule.recurs(5))
                .repeat(Schedule.spaced(10.second)) // every 10 seconds flushes 5 items from queue
                .fork
            _ <- offer.join // necessary
            _ <- take.join // why NOT necessary??
        } yield ()
    }

    def run(): Unit = Main.run(
        theProgram.provideLayer(queueLayer)
    )
}
