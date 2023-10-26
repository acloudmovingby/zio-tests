import java.util.concurrent.TimeUnit
import zio._
import zio.stream.{ZSink, ZStream}

import java.io.IOException

object ParallelTimingExperiments {

    // results of experiments: performance maxed out at parallelism = 6, rechunk made no difference beyond 1

    val elems = List.range(0, 10)

    def runStream(parallelism: Int, sleepTime: Long, rechunk: Int) = for {
        //_ <- Console.printLine(s"parallism=$parallelism, sleepTime=$sleepTime, rechunk=$rechunk")
        start <- Clock.currentTime(TimeUnit.MILLISECONDS)
        _ <- ZStream.fromIterable(elems)
            //.tap(elem => Clock.currentTime(TimeUnit.MILLISECONDS).map(y => println(s"elem=$elem, curTime=$y")))
            .mapZIOPar(parallelism)(_ => zio.Clock.sleep(sleepTime.millis))
            .rechunk(rechunk)
            .run(ZSink.drain)
            //.foreach(elem => Clock.currentTime(TimeUnit.MILLISECONDS).map(y => println(s"elem=$elem, curTime=$y")))
        end <- Clock.currentTime(TimeUnit.MILLISECONDS)
    } yield end - start

    def specifyRechunk(rechunk: Int) = for {
        t1 <- runStream(1, 50L, rechunk)
        t2 <- runStream(2, 50L, rechunk)
        t3 <- runStream(3, 50L, rechunk)
        t4 <- runStream(4, 50L, rechunk)
        t5 <- runStream(5, 50L, rechunk)
        t6 <- runStream(6, 50L, rechunk)
    } yield (t1, t2, t3, t4, t5, t6)

    val rechunk = 6

    val whatToRun = for {
        t1 <- specifyRechunk(1)
        t2 <- specifyRechunk(2)
        t3 <- specifyRechunk(3)
        t4 <-specifyRechunk(4)
        t5 <- specifyRechunk(5)
        t6 <- specifyRechunk(6)
        t7 <- specifyRechunk(7)
        t8 <- specifyRechunk(8)
        t9 <- specifyRechunk(9)
        t10 <- specifyRechunk(10)
        _ <- Console.printLine(s"rechunk=1: p=${t1._1}, p=${t1._2}, p=${t1._3}, p=${t1._4}, p=${t1._5}, p=${t1._6}")
        _ <- Console.printLine(s"rechunk=2: p=${t2._1}, p=${t2._2}, p=${t2._3}, p=${t2._4}, p=${t2._5}, p=${t2._6}")
        _ <- Console.printLine(s"rechunk=3: p=${t3._1}, p=${t3._2}, p=${t3._3}, p=${t3._4}, p=${t3._5}, p=${t3._6}")
        _ <- Console.printLine(s"rechunk=4: p=${t4._1}, p=${t4._2}, p=${t4._3}, p=${t4._4}, p=${t4._5}, p=${t4._6}")
        _ <- Console.printLine(s"rechunk=5: p=${t5._1}, p=${t5._2}, p=${t5._3}, p=${t5._4}, p=${t5._5}, p=${t5._6}")
        _ <- Console.printLine(s"rechunk=6: p=${t6._1}, p=${t6._2}, p=${t6._3}, p=${t6._4}, p=${t6._5}, p=${t6._6}")
        _ <- Console.printLine(s"rechunk=7: p=${t7._1}, p=${t7._2}, p=${t7._3}, p=${t7._4}, p=${t7._5}, p=${t7._6}")
        _ <- Console.printLine(s"rechunk=8: p=${t8._1}, p=${t8._2}, p=${t8._3}, p=${t8._4}, p=${t8._5}, p=${t8._6}")
        _ <- Console.printLine(s"rechunk=9: p=${t9._1}, p=${t9._2}, p=${t9._3}, p=${t9._4}, p=${t9._5}, p=${t9._6}")
        _ <- Console.printLine(s"rechunk=10: p=${t10._1}, p=${t10._2}, p=${t10._3}, p=${t10._4}, p=${t10._5}, p=${t10._6}")
    } yield ()

    def run(): Unit = Main.run(whatToRun)
}
