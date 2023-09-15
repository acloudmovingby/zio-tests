import zio._

object ZLayerExperiments {
    trait NumberGenerator {
        def get: Task[Option[Int]]

        def put(n: Int): Task[Unit]
    }

    class NumberGeneratorLive extends NumberGenerator {
        def get: Task[Option[Int]] = for {
            _ <- Console.printLine("Hey! I'm a program. Give me a number!")
            num <- Console.readLine
            doubled <- ZIO.attempt(num.toInt * 2)
            _ <- Console.printLine(s"The number doubled is: $doubled")
        } yield Some(doubled)

        def put(n: Int): Task[Unit] = Console.printLine(s"Putting $n into the queue")
    }

    object NumberGeneratorLive {
        val layer: ZLayer[Any, Nothing, NumberGeneratorLive] = ZLayer.succeed(new NumberGeneratorLive)
    }

    trait Database {
        def dbName(): String

        def getUserName(id: Int): IO[Throwable, String]
    }

    class DatabaseLive extends Database {
        def dbName(): String = "mockdb"

        def getUserName(id: Int): IO[Throwable, String] = ZIO.succeed(s"user$id")
    }

    object DatabaseLive {
        val layer: ZLayer[Any, Nothing, DatabaseLive] = ZLayer.succeed(new DatabaseLive)
    }

    val theProgram: ZIO[Database with NumberGenerator, Throwable, Unit] = {
        for {
            numQ <- ZIO.service[NumberGenerator]
            num <- numQ.get.map(_.getOrElse(0))
            db <- ZIO.service[Database]
            username <- db.getUserName(num)
            _ <- Console.printLine(s"The username fetched from the database is: $username")
        } yield ()
    }


}
