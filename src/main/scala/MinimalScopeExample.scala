import zio._

import java.io.IOException
import scala.io.Source

object MinimalScopeExample {

    /*
    Goal here is to show the smallest possible example of using ZIO.scoped
    */

    // acquire can fail
    def acquireInt: ZIO[Any, Throwable, Int] = ZIO.attempt {
        println("Acquiring the int!")
        1
    }

    // release takes the int, then must succeed
    def releaseInt: Int => ZIO[Any, Nothing, Any] = (_: Int) => ZIO.succeed {
        println("Releasing int!")
    }

    val getIntScoped = ZIO.acquireRelease(acquireInt)(releaseInt)

    val getInt = ZIO.scoped { getIntScoped }

    def run(): Unit = Main.run(
        getInt
    )
}
