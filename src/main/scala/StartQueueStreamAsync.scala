import StartQueueStreamAsync._
import zio._
import zio.stream.{ZSink, ZStream}
import java.io.IOException

class ClassWithQueue {

    import StartQueueStreamAsync._

    // initialize queue
    val q: Queue[String] = runTask(Queue.bounded[String](100))

    def run = Seq("hello", "world", "hey", "woke", "apple").foreach{ i=>
        println(s"$i -->")
        runTask(q.offer(i))
        Thread.sleep(50)
    }

    // start ZStream from queue
    runTaskFork {
        ZStream.fromQueue(q)
            .tap { i => Console.printLine(s"--> $i") }
            .groupByKey(_.head) { case (k: Char, v: ZStream[Any, IOException, String]) =>
                //println(s"key=$k, values=$v")
                ZStream.fromZIO(v.runCollect.map(l => k -> l.size))
            }
            .tap(i => Console.printLine(s"result=$i"))
            .run(ZSink.drain)
    }
}

object OriginalExperiments {
    def run = {
        runTask(ZStream.fromIterable(Seq("a", "b", "c", "d", "e")).runCollect.map(println(_)))
        runTask(ZStream.fromZIO(Console.printLine("foo")).run(ZSink.drain))
        runTask {
            ZStream.fromIterable(Seq("hello", "world", "hey", "woke", "apple"))
                .tap { i => Console.printLine(s"--> $i") }
                .groupByKey(_.head) { case (k: Char, v: ZStream[Any, IOException, String]) =>
                    //println(s"key=$k, values=$v")
                    ZStream.fromZIO(v.runCollect.map(l => k -> l.size))
                }
                .tap(i => Console.printLine(s"result=$i"))
                .run(ZSink.drain)
        }

        case class Exam(person: String, score: Int)

        val examResults = Seq(
            Exam("Alex", 64),
            Exam("Michael", 97),
            Exam("Bill", 77),
            Exam("John", 78),
            Exam("Bobby", 71)
        )

        runTask {
            ZStream
                .fromIterable(examResults)
                .groupByKey(exam => exam.score / 10 * 10) {
                    case (k, s) => ZStream.fromZIO(s.runCollect.map(l => k -> l.size))
                }
                .tap(i => Console.printLine(s"result=$i"))
                .run(ZSink.drain)
        }
    }
}

object GroupByKeyOfficialExample {
    private def example = {
        case class Exam(person: String, score: Int)

        val examResults = Seq(
            Exam("Alex", 64),
            Exam("Michael", 97),
            Exam("Bill", 77),
            Exam("John", 78),
            Exam("Bobby", 71)
        )

        ZStream
                .fromIterable(examResults)
                .groupByKey(exam => exam.score / 10 * 10) {
                    case (k, s) => ZStream.fromZIO(s.runCollect.map(l => k -> l.size))
                }
                .tap(i => Console.printLine(s"result=$i"))
                .run(ZSink.drain)
    }

    def normal() = runTask(example)
    def forked() = runTaskFork(example)
}

object DemonstrateForkFromZIOBug {

    import StartQueueStreamAsync._

    private def runFromZIONormal(): Unit = {
        println("Expecting 1")
        val z = ZStream.fromZIO(Console.printLine("1")).run(ZSink.drain)
        runTask(z)
    }
    private def runFromZIOForked(): Unit = {
        println("Expecting 2")
        val z = ZStream.fromZIO(Console.printLine("2")).run(ZSink.drain)
        runTaskFork(z)
    }
    private def runFromZIOWithinGroupByKey(): Unit = {
        println("Expecting 3")
        val z = ZStream.fromIterable(Seq("hello"))
            .groupByKey(_.head) { (x, y) =>
                val foo = y.runFold(3)((s, a) => 3).flatMap(x => Console.printLine(x))
                ZStream.fromZIO(foo)
            }
            .run(ZSink.drain)
        runTask(z)

    }
    private def runFromZIOWithinGroupByKeyForked(): Unit = {
        println("Expecting 4")
        val z = ZStream.fromIterable(Seq("hello"))
            .groupByKey(_.head) { (x, y) =>
                val foo = y.runFold(4)((s, a) => 4).flatMap(x => Console.printLine(x))
                ZStream.fromZIO(foo)
            }
            .run(ZSink.drain)
        runTaskFork(z)
    }

    def runAll(): Unit = {
        runFromZIONormal()
        runFromZIOForked()
        runFromZIOWithinGroupByKey()
        runFromZIOWithinGroupByKeyForked()
    }
}

object StartQueueStreamAsync {

    val runtime = Runtime.default

    def runTaskFork[A](program: Task[A]) = Unsafe.unsafe { implicit unsafe =>
        runtime.unsafe.fork(program)
    }

    def runTask[A](program: Task[A]): A = Unsafe.unsafe { implicit unsafe =>
        runtime.unsafe.run(program).getOrThrow()
    }

    private val logIntervl: Int = 2
    private val logIntervalSec: zio.Duration = logIntervl.seconds
    private val zioRuntime = Runtime.default
    var firstTimeStamp: Long = java.lang.System.nanoTime()
    var lastTimeStamp: Long = java.lang.System.nanoTime()
    var msgsReceived: Long = 0L
    var sumOfQueueSizes: Long = 0L

    //private def runTaskFork[A](t: Task[A]) = Unsafe.unsafe { implicit unsafe => zioRuntime.unsafe.fork(t) }

    val msgs = 6520
    val dur = 17390069851L // nanoseconds
    val msgsBD = BigDecimal(msgs)
    val durBD = BigDecimal(dur)
    val ratePerMin = (msgsBD / durBD * BigDecimal(60000000000L)).toInt

    def run(): Unit = {
        //DemonstrateForkFromZIOBug.runAll()
        //GroupByKeyOfficialExample.normal()
        println(s"ratePerMin = $ratePerMin")
//        runTaskFork {
//            ZIO.attempt {
//                this.synchronized {
//                    val duration = lastTimeStamp - firstTimeStamp
//                    val avgQueueSize = if (msgsReceived == 0) 0 else sumOfQueueSizes / msgsReceived
//                    if (msgsReceived > 0) {
//                        println(s"dragon - Received $msgsReceived messages in ${duration / 1000000} milliseconds from ASB; throughput was ${duration / 1000000 / 60} messages per minute (first message at $firstTimeStamp, last message at $lastTimeStamp); average queue size was $avgQueueSize")
//                    } else println(s"dragon - No messages received in last $logIntervl seconds") // TODO remove 'dragon' and also check if debug before logging line
//                    msgsReceived = 0
//                    sumOfQueueSizes = 0L
//                }
//            }.repeat(Schedule.spaced(logIntervalSec))
//        }
        println("FINISHED")
    }
}
