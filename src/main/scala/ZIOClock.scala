import Utils._
import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream.{ZSink, ZStream}

object ZIOClockCommon {
    def printSleep() = for {
        _ <- Clock.currentDateTime.flatMap(Console.printLine(_))
        _ <- Clock.sleep(100.milliseconds)
    } yield ()

    def run() = {
        Main.run(printSleep())
    }
}

object ZIOClock extends ZIOSpecDefault {

    //    def printTimeForever: ZIO[Any, Throwable, Nothing] =
    //        Clock.currentDateTime.flatMap(Console.printLine(_)) *>
    //            ZIO.sleep(1.seconds) *> printTimeForever
    def spec = suite("ZIOClock")(
        test("test TestClock") {
            for {
                fiber <- ZIOClockCommon.printSleep()
                    .repeat((Schedule.elapsed).untilOutput { o =>
                             println(s"o=$o")
                        o > 5.seconds
                        })
                    .flatMap(x => Console.printLine(s"x: $x"))
                    .fork
                _ <- TestClock.adjust(10.seconds)
                foo <- fiber.join
            } yield assertTrue(true)
        } //@@ TestAspect.timeout(5.seconds)
    )
}
