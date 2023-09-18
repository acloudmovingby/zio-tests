import ZLayerExperiments.DatabaseLive
import zio._

object QueueExperiments {

    val q: UIO[Queue[Int]] = Queue.bounded[Int](100)

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
        parsed <- ZIO.attempt(input.toInt)
        _ <- queue.offer(parsed)
        size <- queue.size
        _ <- Console.printLine(s"Queue size=$size")
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
            _ <- Console.printLine("Does print")
            _ <- offer.join // necessary
            _ <- Console.printLine("Never prints")
            //_ <- take.join // why NOT necessary??
            _ <- Console.printLine("Never prints")
        } yield ()
    }

    def printStr(s: String) = Console.printLine(s)

    val theProgram2 = for {
        a <- Console.printLine("A").fork
        b <- {
            ZIO.sleep(15.second) *> Console.printLine("B")
        }.fork
        c <- {
            ZIO.sleep(5.second) *> Console.printLine("C").fork
        }
        _ <-a.join
    } yield ()


    def run(): Unit = Main.run(
        theProgram2
        //theProgram.provideLayer(queueLayer)
    )
}
