import zio.{ZIO, _}

object Main extends App {

    def run[E, A](program: ZIO[Any, E, A]): Unit = Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.run[E, A](program) match {
            case Exit.Success(value) => println(s"Success! value=$value")
            case Exit.Failure(cause) => println(s"Failure! cause=$cause")
        }
    }

    run(
        ZLayerExperiments.theProgram
        .provide(
            ZLayerExperiments.DatabaseLive.layer,
            ZLayerExperiments.NumberGeneratorLive.layer
        )
    )


}