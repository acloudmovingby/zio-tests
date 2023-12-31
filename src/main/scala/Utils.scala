import zio._
import zio.stream.ZStream

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

object Utils {
    def sleepAnd[A](
        z: => A,
        t: Long = 10
    ): ZIO[Any, Nothing, A] = Clock.sleep(t.millis).map(_ => z)

    def sleepAndZIO[R, E, A](
        z: ZIO[R, E, A],
        t: Long = 10
    ): ZIO[R, E, A] = Clock.sleep(t.millis).flatMap(_ => z)

    def printWithTime(msg: Any) = {
        val time = new SimpleDateFormat("HH:mm:ss.SSS")
            .format(new Date(java.lang.System.currentTimeMillis()))
        Console.printLine(s"$time: $msg")
    }

    def streamFromConsoleInput(): ZStream[Any, IOException, String] =
        ZStream.repeatZIO(Console.readLine)
    def streamFromConsoleInput(prompt: String): ZStream[Any, IOException, String] =
        ZStream.repeatZIO(Console.readLine(prompt))
}