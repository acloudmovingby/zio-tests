import zio._

class SuperDuperSimpleClass {
    def a() = {
        b1()
        b2()
    }
    def b1() = {
        c1()
        c2()
    }
    def b2() = {
        c3()
        c4()
    }
    def c1() = ()
    def c2() = ()
    def c3() = ()
    def c4() = ()
}
