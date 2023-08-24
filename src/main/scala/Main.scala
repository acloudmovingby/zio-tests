import zio._

object Main extends App {
    val runtime = Runtime.default

    val program: ZIO[Any, Throwable, UIO[Unit]] = {
        for {
            _ <- Console.printLine("Hey! I'm a program. Give me a number!")
            num <- Console.readLine
            parsed <- ZIO.attempt(num.toInt).map(_ * 2)
            _ <- Console.printLine(s"The number doubled is: $parsed")
            _ <- Console.print("Hey ") *> Console.print("dude\n")
        } yield ZIO.unit
    }

    Unsafe.unsafe { implicit unsafe =>
        runtime.unsafe.run[Throwable, UIO[Unit]](program).getOrElse(e => println(s"Error:${e.failureOption}"))
    }
}