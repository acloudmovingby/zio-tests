import zio.{stream, _}
import zio.stream.{ZSink, ZStream}

import java.io.IOException
import java.util.concurrent.TimeUnit
import java.lang.System
import java.math.BigInteger

object IntermittentLoggingZStream {
    val finiteIntStream: ZStream[Any, Nothing, Int] = ZStream.range(1, 100)

    val batchSizeForRateLogging = 10

    // every N items log the rate, using mapAccum
    // Note: this didn't work as I texpected. The mapAccum actually blocks until the *entire* stream is processed, almost like it's doing a giant rechunk
    val withMapAccum: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- finiteIntStream
                .mapAccum((0L, System.nanoTime()))((acc: (Long, Long), i: Int) => {
                    println(s"${(acc, i)}")
                    if (acc._1 < batchSizeForRateLogging) {
                        println(s"returning ${((acc._1 + 1, acc._2), i)}")
                        ((acc._1 + 1, acc._2), i)
                    } else {
                        val curTime = System.nanoTime()
                        val elapsedTime = curTime - acc._2
                        val rate = batchSizeForRateLogging / elapsedTime * 1000000000
                        println(s"Processed $rate items per second (processed $batchSizeForRateLogging items in $elapsedTime nanoseconds, prevTime=${acc._2}, curTime=$curTime)")
                        ((0L, curTime), i)
                    }
                })
                .tap(i => ZIO.attempt(println(i))) // this did not log until the above mapAccum had logged repeatedly for all elements
                .run(ZSink.drain)
        } yield ()
    }

    var timeStamp: Long = System.nanoTime()

    // every N items, log the rate
    // the problem with this way is that it does require rechunking things which could have a performance impact
    val withMapChunk: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- finiteIntStream
                .rechunk(batchSizeForRateLogging)
                .mapChunks(chunk => {
                    val curTime = System.nanoTime()
                    val elapsedTime = curTime - timeStamp
                    val rate = (batchSizeForRateLogging * 1000000000 * 10) / elapsedTime  * 6
                    println(s"(batchSizeForRateLogging * 1000000000 * 60) = ${(batchSizeForRateLogging * 1000000000 * 60)}")
                    println(s"Processed $rate items per minute (processed $batchSizeForRateLogging items in $elapsedTime nanoseconds, prevTime=$timeStamp, curTime=$curTime)")
                    timeStamp = curTime
                    chunk
                })
                .tap(i => ZIO.attempt(println(i)))
                .run(ZSink.drain)
        } yield ()
    }

    var secondFlag = 0

    val withZippedSchedule: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- ZIO.unit
            secondStream = ZStream.iterate(0)(a => a + 1 % 2) // 0, 1, 0, 1... infinitely...
                .schedule(Schedule.spaced(1.second))
            _ <- finiteIntStream
                .tap(_ => ZIO.sleep(100.millis))
                .zipLatestWith(secondStream)((i, seconds) =>  {
                    if (seconds != secondFlag) { // check when number flips
                        secondFlag = seconds
                        println("time")
                    }
                    i
                })
                .tap(i => ZIO.attempt(println(i)))
                .run(ZSink.drain)
        } yield ()
    }

    var receivedCount: Long = 0L
    var lastRecordedTimeNs: Option[Long] = None
    val nanoSecondsPerSec: Long = 1000000000
    val logFrequency: Long = 20L

    // okay, let's not be fancy, what if we just keep both counter and time in global state
    val simpleGlobalMutableState: ZIO[Any, Throwable, Unit] = {
        for {
            _ <- finiteIntStream
                .tap(_ => ZIO.sleep(100.millis))
                .tap(_ => {
                    synchronized {
                        if (receivedCount == 0L) {
                            lastRecordedTimeNs = Some(java.lang.System.nanoTime())
                        }
                        receivedCount += 1L
                        if (receivedCount > logFrequency) {
                            val now = java.lang.System.nanoTime()
                            lastRecordedTimeNs.foreach(t => {
                                val elapsed = now - t
                                val perMin = logFrequency * nanoSecondsPerSec * 60 / elapsed
                                println(s"Received $logFrequency messages in $elapsed nanoseconds ($perMin/minute, ${perMin / 60}/second)")
                                elapsed
                            })
                            lastRecordedTimeNs = Some(now)
                            receivedCount = 0
                        }
                    }
                    ZIO.unit
                })
                .tap(i => ZIO.attempt(println(i)))
                .run(ZSink.drain)
        } yield ()
    }

    def run(): Unit = Main.run(
        //withMapAccum
        //withMapChunk
        //withZippedSchedule
        simpleGlobalMutableState
    )
}
