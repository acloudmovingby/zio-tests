import zio._

object SequenceTraverseExperiments {
    // GOAL:
    // Given List[ZIO[...]], how do I get ZIO[_, _, List[...]]?
    // I googled it and I couldn't find any built-in traverse/sequence for ZIO
    
    
    val initial: ZIO[Any, Nothing, List[Int]] = ZIO.succeed(List.empty[Int])

    // or this sequence? It continues until the first failure, then stops
    def traverse(l: List[ZIO[Any, Nothing, Int]]): ZIO[Any, Nothing, List[Int]] = {
        l.foldRight(ZIO.succeed(List.empty[Int])) { (zio, acc) =>
            acc.flatMap(list => zio.map(zInt => zInt :: list))
        }
    }
    
    def aZIO(i: Int): ZIO[Any, Nothing, Int] = ZIO.succeed(i)

    val theProgram = for {
        _<- Console.printLine("Running program")
        result <- traverse(List.range(0, 10).map(aZIO))
        _ <- Console.printLine(s"result=$result")
    } yield ()

    def run(): Unit = Main.run(
        theProgram
    )
}
