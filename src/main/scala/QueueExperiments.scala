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
        _ <- Console.printLine("Enter a number or type \"exit\" to stop running program.")
        input <- Console.readLine
        _ <- input match {
            case "stop" => for {
                size <- queue.size
                _ <- Console.printLine(s"Queue size=$size. Shutting down...")
                _ <- ZIO.sleep(100.millis).repeatUntilZIO(_ => queue.isEmpty).flatMap(_ => queue.shutdown)
            } yield ()
            case _ => for {
                parsed <- ZIO.attempt(input.toInt)
                _ <- Console.printLine(s"Offering item to queue (item=$parsed)")
                _ <- queue.offer(parsed)
                size <- queue.size
                _ <- Console.printLine(s"Current queue size=$size")
            } yield ()
        }
    } yield ()

    val takeFromQueue: ZIO[Queue[Int], Throwable, Unit] = for {
        queue <- ZIO.service[Queue[Int]]
        _ <- queue.take.flatMap(i => Console.printLine(s"Removing item from queue. (item=${i.toString})"))
    } yield ()

    val theProgram: ZIO[Queue[Int], Throwable, Unit] = {
        for {
            offer <- readInputOfferToQueue.repeat(Schedule.forever) // asks for number on console, adds to queue
                .fork
            take <- {
                // every x seconds flush y items from queue if able (flushing at interval of z millis per item)
                val x = 5.second
                val y = 5
                val z = 200.millis
                takeFromQueue
                    .repeat(Schedule.spaced(z) >>> Schedule.recurs(y))
                    .repeat(Schedule.spaced(x))
                    .fork
            }
            _ <- offer.join // necessary
            _ <- take.join // why NOT necessary??
        } yield ()
    }

    def run(): Unit = Main.run(
        theProgram.provideLayer(queueLayer)
    )
}
