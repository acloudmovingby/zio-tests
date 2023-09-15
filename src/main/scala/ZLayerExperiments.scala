import zio._

object ZLayerExperiments {
    trait NumberGenerator {
        def get: Task[Int]

        def put(n: Int): Task[Unit]
    }

    object NumberGenerator {
        def get: ZIO[NumberGenerator, Throwable, Int] = ZIO.serviceWithZIO[NumberGenerator](_.get)

        def put(n: Int): ZIO[NumberGenerator, Throwable, Unit] = ZIO.serviceWithZIO[NumberGenerator](_.put(n))
    }

    trait Database {
        def dbName(): String

        def getUserName(id: Int): IO[Throwable, String]
    }

    object Database {
        def dbName(): ZIO[Database, Nothing, String] = ZIO.serviceWith[Database](_.dbName())

        def getUserName(id: Int): ZIO[Database, Throwable, String] = ZIO.serviceWithZIO[Database](_.getUserName(id))
    }


    class NumberGeneratorLive extends NumberGenerator {
        def get: Task[Int] = for {
            _ <- Console.printLine("Give me a number!! (must be less than 100)")
            input <- Console.readLine
            num <- ZIO.attempt(input.toInt)
        } yield num

        def put(n: Int): Task[Unit] = Console.printLine(s"Putting $n into the queue")
    }

    object NumberGeneratorLive {
        val layer: ZLayer[Any, Nothing, NumberGeneratorLive] = ZLayer.succeed(new NumberGeneratorLive)
    }

    class DatabaseLive extends Database {
        def dbName(): String = "mockdb"

        def getUserName(id: Int): IO[Throwable, String] = if (id < 100) {
            ZIO.succeed(s"user$id")
        } else {
            ZIO.fail(new Exception(s"No users with id=$id. Id must be <100"))
        }
    }

    object DatabaseLive {
        val layer: ZLayer[Any, Nothing, DatabaseLive] = ZLayer.succeed(new DatabaseLive)
    }

    val theProgram: ZIO[Database with NumberGenerator, Throwable, Unit] = {
        for {
            num <- NumberGenerator.get
            username <- Database.getUserName(num) // TODO make this an option and handle as an erro
            _ <- Console.printLine(s"The username fetched from the database is: $username")
        } yield ()
    }


}
