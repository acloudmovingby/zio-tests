import java.io.IOException
import java.time.OffsetDateTime
import zio.{ZIO, _}
import zio.stream.{ZSink, ZStream}
import zio.Schedule.{count, WithState}
import zio.Schedule.Decision.Done

object ScheduleExperiments {

    var blowUp = true
    private def printStuff(msg: String): Task[Option[String]] = ZIO.attempt {
        println(msg)
//        if (blowUp) {
//            blowUp = false
            throw new Exception("what")
//        }
        Some(msg)
    }

    private val metronomeSchedule = Schedule.fixed(100.millis)
    private val schedule1 = Schedule.spaced(2000.millis)
    private val schedule2 = Schedule.spaced(500.millis)
    private val union = schedule1 || Schedule.spaced(300.millis)
    private val intersection = schedule1 && schedule2

    object BasicSchedules {
        val fixed: WithState[(Option[(Long, Long)], Long), Any, Any, Long] = Schedule.fixed(1.second)
        val spaced: WithState[Long, Any, Any, Long] = Schedule.spaced(1.second)
    }

    private def setTimeout(timeout: Duration) =
        Schedule.elapsed.untilOutput(_ > timeout)

    private val exponential = Schedule.exponential(10.millis)

    val foo: WithState[Option[OffsetDateTime], Any, Any, zio.Duration] = Schedule.elapsed.untilOutput(_ > 20.seconds)

    val a: WithState[Long, Any, Any, zio.Duration] = exponential
    val b: WithState[Long, Any, Any, Long] = Schedule.spaced(2.seconds)
    val c: WithState[Option[OffsetDateTime], Any, Any, zio.Duration] = Schedule.elapsed.untilOutput(_ > 20.seconds)
    Schedule.upTo(20.seconds)
    val ab: WithState[(Long, Long), Any, Any, (zio.Duration, Long)] = a && b
    val ba: WithState[(Long, Long), Any, Any, (Long, zio.Duration)] = b && a
    val bc: WithState[(Long, Option[OffsetDateTime]), Any, Any, (Long, zio.Duration)] = b && c
    val cb: WithState[(Option[OffsetDateTime], Long), Any, Any, (zio.Duration, Long)] = c && b
    val ac: WithState[(Long, Option[OffsetDateTime]), Any, Any, (zio.Duration, zio.Duration)] = a && c
    val abc: WithState[((Long, Long), Option[OffsetDateTime]), Any, Any, (zio.Duration, Long, zio.Duration)] = a && b && c

    // The ZIO Schedules, when used with the combinators && || produce absurdly complicated types.
    // This type alias just shows the final return type that happens as you run it (total duration, number of tries, etc.)
    type ScheduleReturning[A] = WithState[_, Any, Any, A]


    // Infinite schedule where intervals grow exponentially, but are capped at 2 seconds. Ignore output (make Unit)
    // the || means "use exponential intervals OR 2 second intervals, whichever arrives first"
    private val cappedExponential: ScheduleReturning[Unit] =
        (Schedule.exponential(10.millis) || Schedule.spaced(2.seconds)).unit

    private val retrySchedule: ScheduleReturning[(Duration, Long)] = {
        // this means: must be within cappedExponential schedule AND within 10 seconds AND return the output from the "forever" Schedule
        cappedExponential &&
        Schedule.upTo(5.seconds) &&
        Schedule.forever                       // only using this because it outputs the # of tries
    }.tapOutput { case (totalTime, retries) =>
        Console.printLine(s"Retry $retries, time elapsed: ${totalTime.toSeconds}sec").orDie
    }
    //priva val otherSchedule: WithState[((Long, Option[OffsetDateTime]), Long), Any, Any, (zio.Duration, zio.Duration, Long)]
    //priva val otherSchedule: WithState[((Long, Long), Option[OffsetDateTime]), Any, Any, (zio.Duration, Long, zio.Duration)]
    private val otherSchedule: WithState[((Long, Long), Long, Boolean), Any, Any, Any] = {
        exponential && Schedule.forever// && Schedule.upTo(20.seconds)
    }.whileOutput(_._1 < 5.seconds).andThen(Schedule.spaced(5.seconds)).tapOutput { case (out1, out2, out3) =>
        // out1 = exponential output (current sleep)
        // out2 = forever output (# tries)
        // out3 = total time
        Console.printLine(s"out1=$out1 out2=$out2 out3=$out3").orDie
    }

    private val what: WithState[(Option[(Long, Long)], Long), Any, Any, Long] = Schedule.fixed(5.seconds)
    private val foo1: WithState[Long, Any, Any, zio.Duration] = exponential
    private val foo2: WithState[Long, Any, Any, zio.Duration] = exponential.whileOutput(_ < 5.seconds)
    private val foo3/*: WithState[(Long, (Option[(Long, Long)], Long), Boolean), Any, Any, Any]*/ =
        exponential.whileOutput(_ < 3.seconds).andThen(Schedule.fixed(3.seconds) && Schedule.elapsed)
            .tapOutput {
                case z: zio.Duration => Console.printLine(s"(duration) z=$z").orDie
                case (l: Long, z: zio.Duration) => Console.print(s"(long) l=$l z=$z").orDie
                case other => Console.printLine(s"other=$other").orDie
            }

    private val foof = {
        ((Schedule.exponential(1.millis) || Schedule.spaced(2.seconds)).unit) && // exponentially growth capped at 2 seconds
            Schedule.upTo(8.seconds) && // timeout after 5 seconds
            Schedule.forever
    }.tapOutput { case (duration: Duration, tries: Long) =>
        Console.printLine(s"tries=$tries duration=$duration").orDie
    }

    private val s1 = Schedule.exponential(1.millis).tapOutput(x => Console.printLine(s"exponential=$x").orDie)
    private val s2 = Schedule.fixed(1.second).tapOutput(s => Console.printLine(s"fixed=$s").orDie)
    private val s3 = ((s1 || s2) && Schedule.upTo(8.seconds))

    private val z1 = ZIO.attempt(3)
    private val z2 = ZIO.attempt("hey")

    private val decisionSchedj = Schedule.upTo(4.seconds).onDecision {
        case (input, value, Done) => Console.printLine(s"Done! FirstCondition: ${input}, ${value}").orDie
        case _ => ZIO.unit
    }
//    .reconsider {
//        case (input, value, decision) => Right((input, 4.seconds))
//    }

    Schedule.recurUntil[Float](float => float > 8)
        .repetitions
        .whileOutput(repetition => repetition < 3)
        .passthrough[Float]
        .onDecision {
            case (input, value, decision) => Console.printLine(s"FirstCondition: ${input}, ${value}, ${decision}").orDie
        }

    val zippedZs: ZIO[Any, Throwable, (Int, String)] = z1.zip(z2)

    private val exponentialBackoff = Schedule.exponential(1.second)



    /** requirements:
     * - (DONE) retry exponentially with cap
     * - (DONE) with timeout
     * - log warn on first failure
     * - log error every minute or 2 after that
    */



    private var i = 0
    private val metronomeTick = for {
        _ <- ZIO.attempt(i += 1)
        _ <- ZIO.attempt(println((i * 100).toString))
    } yield ()

    // ZIO[-R, +E, +A]

    // every 60 seconds is 60 * 1000, every 6000 millisecond

    val s: WithState[(Option[OffsetDateTime], Long), Any, Any, (zio.Duration, Long)] = Schedule.upTo(100.millis) && Schedule.forever
    private val theProgram = for {
        //metronome <- metronomeTick.repeat(metronomeSchedule).fork
        foo <- printStuff("a")
//            .onError { e1 =>
//                println(s"first time errored: ${e1.squash}")
//                printStuff("b").retry(Schedule.exponential(10.millis).whileOutput(_ < 1.second))
//                    .onError { e2 =>
//                        printStuff("c").retry(Schedule.fixed(1.seconds)).ignore
//                    }
//            }
            .retry((s1 || s2) && decisionSchedj)//.repeat(retrySchedule)
        //(duration, tries) = foo
    } yield {
        //println(s"${duration.toMillis}ms, $tries tries")
    }

    def run(): Unit = Main.run(
        theProgram
    )
}
