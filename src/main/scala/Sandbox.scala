import java.io.IOException
import zio._
import zio.stream.{ZSink, ZStream}
import zio.Schedule.WithState

trait LoadProgress

trait NotificationBucketEnqueuer {
    type NotificationBucket = Int
    protected val loadOperation: Task[(NotificationBucket, LoadProgress)]
    protected val isFinishedLoading: LoadProgress => Boolean

    def enqueueStream(): ZIO[Any, Throwable, Unit] = {
        ZStream.fromZIO(loadOperation)
            .repeat(Schedule.recurUntil { case (_: NotificationBucket, progress: LoadProgress) =>
                isFinishedLoading(progress)
            case _ => ZIO.fail(new IOException("Unexpected LoadProgress type"))
            })
            .map { case (bucket, _) => bucket }
            .tap { bucket =>
                Console.printLine(s"tapping stream: $bucket")
            }
            .run(ZSink.drain)
    }
}

case class RowsLoaded(rows: Int) extends LoadProgress

class PassUpdateASBEnqueuer extends NotificationBucketEnqueuer {
    var counter = 0

    def dbOperation(): (NotificationBucket, LoadProgress) = {
        counter += 1
        println(s"DB operation $counter")
        val rowsLoaded = if (counter < 5) 1 else 0
        (counter, RowsLoaded(rowsLoaded))
    }

    protected val loadOperation: Task[(NotificationBucket, LoadProgress)] = ZIO.attempt(dbOperation())
    protected val isFinishedLoading: LoadProgress => Boolean = {
        case RowsLoaded(rows) => rows == 0
    }
}

object Sandbox {

    var counter = 0
    var isRunning = true

    def dbOperation(): Int = {
        counter += 1
        println(s"DB operation $counter")
        counter
    }

    val dbOperationZIO: ZIO[Any, Throwable, Int] = ZIO.attempt(dbOperation())

    val dbOperationStream: ZStream[Any, Throwable, Int] =
        ZStream.fromZIO(dbOperationZIO)
            .repeat(Schedule.recurUntil(_ => counter >= 5 && isRunning))

    def runStream(z: ZStream[Any, Throwable, Int]): ZIO[Any, Throwable, Unit] = {
        z.tap { _ =>
                Console.printLine(s"tapping stream: $counter")
            }
            .run(ZSink.drain)
    }

    def resetCounters(): Unit = {
        counter = 0
        isRunning = true
    }

    val enqueuer = new PassUpdateASBEnqueuer()

    val theProgram: ZIO[Any, Throwable, Unit] = for {
        _ <- Console.printLine("The Stream:")
        _ = resetCounters()
        _ <- runStream(dbOperationStream)
        _ <- Console.printLine("The Stream has finished")
        _ <- enqueuer.enqueueStream()
    } yield ()

    def run(): Unit = Main.run(
        theProgram
    )
}
