package async4s

import org.async4j.Callback
import org.async4j.FutureCallback

class PipeGen[R](f: (R => Unit) => Unit) {
  def foreach(next: (R => Unit)) = f(next)
  def filter(x: (R => Unit)) = this
}

object PipeGen {
  def pipe[R](f: (R => Unit) => Unit) = new PipeGen[R](f)
  def test = {
    val i = 2;
    for {
      r <- pipe((next: (Int => Unit)) => { next(i * i) })
      s <- pipe((next: (Int => Unit)) => { next(i + r) })
      _ <- pipe((next: (Unit => Unit)) => { println(r + s) })
    } {}
  }
}

class Pipe[R](k: Callback[_], f: PartialFunction[R => Unit, Unit]) {
  def foreach(next: (R => Unit)) = try {
    f(next)
  } catch { case e => k.error(e) }

  def filter(x: (R => Unit)) = this
}

object Pipe {
  def pipe[R](k: Callback[_])(f: PartialFunction[R => Unit, Unit]) = new Pipe[R](k, f)

  def test = {
    val k = new FutureCallback[Unit]
    val i = 2;
    for {
      r <- pipe[Int](k) {
        case next =>

          next(i * i)
      }

      s <- pipe[Int](k) {
        case next =>

          next(i + r)
      }

      _ <- pipe[Unit](k) {
        case next =>
          println(r + s)
          next()
      }
    } { k.completed() }

    k.getResult()
  }
}

