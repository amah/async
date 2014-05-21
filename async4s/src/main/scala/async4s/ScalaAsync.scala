package async4s

import java.util.concurrent.atomic.AtomicBoolean
import scala.actors.threadpool.Executor
import scala.concurrent.ops._
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import java.lang.Throwable
import scala.actors.threadpool.AtomicInteger
import org.async4j.Callback
object async {
  def parallelFor[I](pk: Callback[Unit], g: Producer[I], max: Int, task: (Callback[Unit], I) => Unit) = {
    try {
      new Loop().start(pk, g, max, task)
    } catch { case e => pk.error(e) }
  }

  def _try[R](k: Callback[R]): (Callback[R] => Unit) => Catch[R] = {
    f: (Callback[R] => Unit) => new Catch[R](k, f)
  }

  def cont[Q, R](k: Callback[R], next: Q => Unit) = new ContinuationGenerator[Q, R](k, next)

  def forkjoin[I, U, W](pk: Callback[W], t: Traversable[I], max: Int, acc: Accumulotor[U, Unit, W]) = { new ForkJoinGenerator(pk, new IteratorProducer(t), max, null, acc) }
  def sto[U, V, W](pk: Callback[V], acc: Accumulotor[U, V, W]) = { new AccumulatorStoreGenerator(pk, acc) }
  def fwd[U, V, W](pk: Callback[W], acc: Accumulotor[U, V, W]) = { new AccumulatorForwardGenerator(pk, acc) }
  def pipe[R, S](pk: Callback[S], task: (Callback[R]) => Unit) = { new PipeGenerator(pk, task) }
  def _retry[R](pk: Callback[R], retryCount: Int) = { new RetryGenerator(pk, retryCount) }

  def parallel[I](pk: Callback[Unit], g: Producer[I], max: Int) = { new AsyncGenerator(pk, g, max) }
  def parallel[I](pk: Callback[Unit], ae: AsynchronousEnumerator[I], max: Int) = { new AsyncGenerator(pk, new AsynchronousEnumeratorProducer(ae), max) }
  def parallel[I](pk: Callback[Unit], t: Traversable[I], max: Int) = { new AsyncGenerator(pk, new IteratorProducer(t), max) }
  def parallel[I](pk: Callback[Unit], iter: Iterator[I], max: Int) = { new AsyncGenerator(pk, new IteratorProducer(iter), max) }

  def parallel[I](pk: Callback[Unit], g: Producer[I], max: Int, e: Executor) = { new AsyncPooledGenerator(pk, g, max, e) }
  def parallel[I](pk: Callback[Unit], ae: AsynchronousEnumerator[I], max: Int, e: Executor) = { new AsyncPooledGenerator(pk, new AsynchronousEnumeratorProducer(ae), max, e) }
  def parallel[I](pk: Callback[Unit], t: Traversable[I], max: Int, e: Executor) = { new AsyncPooledGenerator(pk, new IteratorProducer(t), max, e) }
  def parallel[I](pk: Callback[Unit], iter: Iterator[I], max: Int, e: Executor) = { new AsyncPooledGenerator(pk, new IteratorProducer(iter), max, e) }
  def parallelTask(pk: Callback[Unit], e: Executor, tasks: (Callback[Unit]) => Unit*) = {
    try {
      for ((k, task) <- parallel(pk, tasks, Int.MaxValue, e)) {
        try {
          task(k)
        } catch { case t => k.error(t) }
      }
    } catch { case t => pk.error(t) }
  }

  def _do[R](body: (Callback[R]) => Unit) = new Do(body)
  def until[R](pk: Callback[Unit], cond: (Callback[Boolean], R) => Unit) = new UntilGenerator(pk, cond)
  def until[R](pk: Callback[Unit], cond: (R) => Boolean) = {
    new UntilGenerator(pk,
      (k: Callback[Boolean], r: R) =>
        try {
          k.completed(cond(r))
        } catch { case t => k.error(t) })
  }
}

abstract class CallbackBase[S, R](k: Callback[R]) extends Callback[S] {
  def error(e: Throwable) = { k.error(e) }
}

class SynchronousCallback[R] extends Callback[R] {
  var lock: AnyRef = new Object()
  var ended: Boolean = false
  var r: R = _
  var e: Throwable = null

  def completed(r: R): Unit = {
    this.r = r;
    unlock
  }

  def error(e: Throwable) {
    this.e = e;
    unlock
  }

  private def unlock: Unit = {
    lock.synchronized {
      ended = true
      lock.notify()
    }
  }

  def result: R = {
    lock.synchronized {
      if (!ended) lock.wait()
    }

    if (this.e != null)
      throw new RuntimeException(e)

    return r
  }

  def await: R = result
}
/** Asynchronous exception management constructs */
class Catch[R](k: Callback[R], body: (Callback[R]) => Unit) {
  def _catch(f: ((Callback[R], Throwable)) => Unit) = {
    body(new CatchCallback(k, f))
  }
  def _finally(f: Callback[Unit] => Unit) = {
    body(new FinalCallback(k, f))
  }
  def _finally(f: => Unit) = {
    body(new FinalCallback(k,
      k => try {
        f; k.completed()
      } catch { case t => k.error(t) }))
  }
}

class CatchCallback[R](pk: Callback[R], f: ((Callback[R], Throwable)) => Unit) extends Callback[R] {
  def completed(result: R) = { pk.completed(result) }
  def error(e: Throwable) = { f.apply(pk, e) }
}

class FinalCallback[R](pk: Callback[R], f: Callback[Unit] => Unit) extends Callback[R] {
  def completed(r: R) = {
    try {
      // TODO use wildcard for R
      val k = new Callback[Unit] {
        def completed(unused:Unit) = { pk.completed(r) }
        def error(ex: Throwable) = { pk.error(ex) }
      }
      f.apply(k)
    } catch { case ex => pk.error(ex) }
  }

  def error(e: Throwable) = {
    try {
      val k = new Callback[Unit] {
        def completed(unused:Unit) = { pk.error(e) }
        def error(ex: Throwable) = { pk.error(ex) }
      }
      f.apply(k)
    } catch { case ex => pk.error(ex) }
  }
}

/** Scala continuation adapter */
class ContinuationCallback[Q, R](k: Callback[R], next: (Q) => Unit) extends CallbackBase[Q, R](k) {
  def completed(q: Q) = try { next(q) } catch { case t => k.error(t) }
}

class ContinuationGenerator[Q, R](pk: Callback[R], next: (Q) => Unit) {
  def foreach(task: (Callback[Q]) => Unit) = try {
    task(new ContinuationCallback[Q, R](pk, next))
  } catch { case e => pk.error(e) }

  def filter(x: (Callback[Q]) => Unit) = this
}

/** Accumulator objects */
trait Accumulotor[U, V, W] {
  def add(k: Callback[V], u: U)
  def value(k: Callback[W])
}

class ThreadPooledAccumulotor[U, V, W](e: Executor, val acc: Accumulotor[U, V, W]) extends Accumulotor[U, V, W] {
  def add(k: Callback[V], u: U) {
    try {
      e.execute(new Runnable() {
        def run = acc.add(k, u)
      })
    } catch { case t => k.error(t) }
  }
  def value(k: Callback[W]) {
    try {
      e.execute(new Runnable() {
        def run = acc.value(k)
      })
    } catch { case t => k.error(t) }
  }
}

class Holder[U](@volatile var data: U) extends Accumulotor[U, U, U] {
  def add(k: Callback[U], u: U) = try {
    data = u;
    k.completed(u)
  } catch { case t => k.error(t) }

  def value(k: Callback[U]) = try {
    k.completed(data)
  } catch { case t => k.error(t) }
}

class AccumulatorStoreGenerator[U, V, W](pk: Callback[V], acc: Accumulotor[U, V, W]) {
  def foreach(task: (Callback[U]) => Unit) = try {
    task(new CallbackBase[U, V](pk) {
      def completed(u: U) = try {
        acc.add(pk, u)
      } catch { case e => pk.error(e) }
    })
  } catch { case e => pk.error(e) }

  def filter(x: (Callback[U]) => Unit) = this
}

class AccumulatorForwardGenerator[U, V, W](pk: Callback[W], acc: Accumulotor[U, V, W]) {
  def foreach(task: (Callback[Unit]) => Unit) = try {
    task(new CallbackBase[Unit, W](pk) {
      def completed(u: Unit) = try {
        acc.value(pk)
      } catch { case e => pk.error(e) }
    })
  } catch { case e => pk.error(e) }

  def filter(x: (Callback[U]) => Unit) = this
}

/** Parallel for constructs */
class AsyncGenerator[I](pk: Callback[Unit], g: Producer[I], max: Int) {
  def foreach(task: ((Callback[Unit], I)) => Unit) = {
    try {
      new Loop().start(pk, g, max, (k: Callback[Unit], i: I) => task((k, i)))
    } catch { case e => pk.error(e) }
  }
  def filter(x: ((Callback[Unit], I)) => Unit) = this
}

class AsyncPooledGenerator[I](pk: Callback[Unit], g: Producer[I], max: Int, pool: Executor) {
  def foreach(task: ((Callback[Unit], I)) => Unit) = {
    try {
      val f = (k: Callback[Unit], i: I) => {
        try {
          pool.execute(new Runnable() { def run = { try { task((k, i)) } catch { case e => k.error(e) } } })
        } catch { case e => k.error(e) }
      }
      new Loop().start(pk, g, max, f)
    } catch { case e => pk.error(e) }
  }
  def filter(x: ((Callback[Unit], I)) => Unit) = this
}

class ForkJoinGenerator[I, U, W](pk: Callback[W], g: Producer[I], max: Int, pool: Executor, acc: Accumulotor[U, Unit, W]) {
  def foreach(task: ((Callback[U], I)) => Unit) = {
    try {
      val f = (k: Callback[Unit], i: I) => {
        try {
          val addK = new CallbackBase[U, Unit](k) {
            def completed(u: U) = try { acc.add(k, u) } catch { case e => k.error(e) }
          }
          pool match {
            case null => task((addK, i))
            case _ => pool.execute(new Runnable() { def run = { try { task((addK, i)) } catch { case e => k.error(e) } } })
          }
        } catch { case e => k.error(e) }
      }
      val valueK = new CallbackBase[Unit, W](pk) {
        def completed(u: Unit) = try { acc.value(pk) } catch { case e => pk.error(e) }
      }
      new Loop().start(valueK, g, max, f)
    } catch { case e => pk.error(e) }
  }
  def filter(x: ((Callback[Unit], I)) => Unit) = this
}

trait Producer[I] {
  def generate(k: Callback[Unit], handler: (Callback[Unit], I) => Unit)
}

trait AsynchronousEnumerator[I] {
  def next(k: Callback[(Boolean, I)])
}

class AsynchronousEnumeratorProducer[I](enumerator: AsynchronousEnumerator[I]) extends Producer[I] {

  def generate(k: Callback[Unit], handler: (Callback[Unit], I) => Unit) = {
    enumerator.next(new NextCallback(k, handler))
  }

  class NextCallback(pk: Callback[Unit], handler: (Callback[Unit], I) => Unit) extends Callback[Tuple2[Boolean, I]] {
    val handlerK = new PartHandlerCallback(pk, this)

    def completed(r: (Boolean, I)) = r match {
      case (true, item) => handler(handlerK, item)
      case _ => pk.completed()
    }

    def error(e: Throwable) = { pk.error(e) }
  }

  class PartHandlerCallback(pk: Callback[Unit], nextK: Callback[Tuple2[Boolean, I]]) extends Callback[Unit] {
    def completed(r: Unit) = { enumerator.next(nextK) }
    def error(e: Throwable) = { pk.error(e) }
  }
}

class IteratorEnumerator[I](iter: Iterator[I]) extends AsynchronousEnumerator[I] {
  def next(k: Callback[(Boolean, I)]) {
    try {
      val hasNext = iter.hasNext
      k.completed(hasNext, hasNext match {
        case true => iter.next()
        case false => null.asInstanceOf[I]
      })
    } catch { case e => k.error(e) }
  }
}

trait AsynchronousIterator[I] {
  def hasNext(k: Callback[Boolean])
  def next(k: Callback[I])
}

class AsynchronousIteratorProducer[I](iter: AsynchronousIterator[I]) extends Producer[I] {

  def generate(k: Callback[Unit], handler: (Callback[Unit], I) => Unit) = {
    iter.hasNext(new HasNextCallback(k, handler))
  }

  class HasNextCallback(pk: Callback[Unit], handler: (Callback[Unit], I) => Unit) extends Callback[Boolean] {
    val nextK = new NextCallback(pk, this, handler)
    def completed(r: Boolean) = r match {
      case true => iter.next(nextK)
      case _ => pk.completed()
    }
    def error(e: Throwable) = { pk.error(e) }
  }

  class NextCallback(pk: Callback[Unit], hasNextK: Callback[Boolean], handler: (Callback[Unit], I) => Unit) extends Callback[I] {
    val handlerK = new PartHandlerCallback(pk, hasNextK)
    def completed(item: I) = { handler(handlerK, item) }
    def error(e: Throwable) = { pk.error(e) }
  }

  class PartHandlerCallback(pk: Callback[Unit], hasNextK: Callback[Boolean]) extends Callback[Unit] {
    def completed(r: Unit) = { iter.hasNext(hasNextK) }
    def error(e: Throwable) = { pk.error(e) }
  }
}

class IteratorProducer[I](iter: Iterator[I]) extends Producer[I] {
  def this(list: Traversable[I]) = this(list.toIterator)

  def generate(k: Callback[Unit], handler: (Callback[Unit], I) => Unit) = {
    new NextCallback(k, handler).next
  }

  class NextCallback(pk: Callback[Unit], handler: (Callback[Unit], I) => Unit) extends Callback[Unit] {
    def next() = {
      try {
        iter.hasNext match {
          case true => handler(this, iter.next())
          case _ => pk.completed()
        }
      } catch { case e => pk.error(e) }
    }

    def completed(r: Unit) = { next }
    def error(e: Throwable) = { pk.error(e) }
  }
}

class Loop() {
  val runningCount: AtomicInteger = new AtomicInteger
  val splitEnded: AtomicBoolean = new AtomicBoolean
  val ended: AtomicBoolean = new AtomicBoolean
  @volatile var err: Throwable = _

  def start[I](pk: Callback[Unit], g: Producer[I], max: Int, task: (Callback[Unit], I) => Unit): Unit = {
    try {
      val controller = new ConcurrentParallelForeachController[I](new TaskEndCallback(pk), runningCount, max)
      g.generate(new SplitterEndCallback(pk), (k: Callback[Unit], i: I) => { controller.run(k, task, i) })
    } catch { case e => pk.error(e) }
  }

  def loopEnd(pk: Callback[Unit]) = {
    if (splitEnded.get() && runningCount.get() == 0) {
      if (!ended.getAndSet(true)) {
        if (runningCount.get() > 0) println("Illegal state: runningCOunt=" + runningCount.get())
        if (err != null) {
          pk.error(err)
        } else {
          pk.completed()
        }
      }
    }
  }

  class SplitterEndCallback(pk: Callback[Unit]) extends Callback[Unit] {
    def completed(r: Unit) {
      splitEnded.set(true)
      loopEnd(pk)
    }

    def error(e: Throwable) {
      err = e
      splitEnded.set(true)
      loopEnd(pk)
    }
  }

  class TaskEndCallback(pk: Callback[Unit]) extends Callback[Unit] {
    def completed(r: Unit) {
      loopEnd(pk)
    }

    def error(e: Throwable) {
      err = e
      loopEnd(pk)
    }
  }
}

class Iter[I](val k: Callback[Unit], val task: (Callback[Unit], I) => Unit, val item: I)

class ConcurrentParallelForeachController[I](pk: Callback[Unit], runningCount: AtomicInteger, max: Int) {
  val taskCallback: Callback[Unit] = new TaskCallback()
  val pendingIterQ = new ConcurrentLinkedQueue[Iter[I]]()
  @volatile var err: Throwable = _
  val lock = new ReentrantLock

  def run(k: Callback[Unit], task: (Callback[Unit], I) => Unit, item: I) = {
    try {
      // TODO check whether a parallel error occurred
      if (runningCount.incrementAndGet() <= max) {
        task(taskCallback, item)
        k.completed()
      } else {
        var wait = false
        lock.lock()
        try {
          if (runningCount.get() > max) {
            pendingIterQ.add(new Iter(k, task, item))
            wait = true;
          } else {
            wait = false;
          }
        } finally {
          lock.unlock()
        }

        if (!wait) {
          task(taskCallback, item)
          k.completed()
        } else {
          if (runningCount.get() == 1) {
            resume()
          }
        }
      }
    } catch { case e => k.error(e) }
  }

  class TaskCallback extends Callback[Unit] {
    def completed(r: Unit) = try {
      val remaining = runningCount.decrementAndGet()
      if (remaining == max) {
        //      println("end:"+runningCount.get())
        resume()
      } else if (remaining == 0) {
        resume()
      }
      pk.completed()
    } catch { case t => pk.error(t) }

    def error(e: Throwable) = try {
      err = e;
      val remaining = runningCount.decrementAndGet();
      if (remaining == max - 1) {
        resume()
      }
      pk.error(e);
    } catch { case t => pk.error(t) }
  }

  // TODO Catch exception or clarify 
  // that the caller must manage the exception
  def resume(): Unit = {
    var iter: Iter[I] = null

    if (!pendingIterQ.isEmpty() && runningCount.get() <= max) {
      lock.lock();
      try {
        if (!pendingIterQ.isEmpty() && runningCount.get() <= max) {
          iter = pendingIterQ.poll()
        }
      } finally {
        lock.unlock();
      }

      if (iter != null) {
        if (err == null) {
          iter.task(taskCallback, iter.item);
          iter.k.completed()
        } else {
          val remaining = runningCount.decrementAndGet();
          if (remaining == max - 1) {
            resume()
          }

          iter.k.error(err);
        }
      }
    }
  }

}

class ParallelForeachController[I](pk: Callback[Unit], runningCount: AtomicInteger, max: Int) {
  val lock: ReentrantLock = new ReentrantLock()
  val taskCallback: Callback[Unit] = new TaskCallback()
  @volatile var pendingTask: (Callback[Unit], I) => Unit = _
  @volatile var pendingItem: I = _
  @volatile var pendingCallback: Callback[Unit] = _
  @volatile var err: Throwable = _

  def run(k: Callback[Unit], task: (Callback[Unit], I) => Unit, item: I) = {
    try {
      // TODO check whether a parallel error occurred
      if (runningCount.incrementAndGet() <= max) {
        task(taskCallback, item)
        k.completed()
      } else {
        var wait = false

        lock.lock();
        try {
          if (runningCount.get() > max) {
            this.pendingTask = task;
            this.pendingCallback = k;
            this.pendingItem = item;
            wait = true;
          } else {
            wait = false;
          }
        } finally {
          lock.unlock();
        }

        if (!wait) {
          task(taskCallback, item)
          k.completed()
        } else {
          if (runningCount.get() == 1) {
            lock.lock();
            try {
              if (runningCount.get() == 1) {
                resume()
              }
            } finally {
              lock.unlock();
            }
          }
        }
      }
    } catch { case e => k.error(e) }
  }

  class TaskCallback extends Callback[Unit] {
    def completed(r: Unit) = {
      val remaining = runningCount.decrementAndGet()
      if (remaining == max) {
        resume()
      }
      pk.completed()
    }

    def error(e: Throwable) = {
      err = e;
      val remaining = runningCount.decrementAndGet();
      if (remaining == max) {
        resume()
      }

      pk.error(e);
    }
  }

  def resume(): Unit = {
    var resumeTask: (Callback[Unit], I) => Unit = null
    var resumeItem: I = null.asInstanceOf[I]
    var resumeCallback: Callback[Unit] = null

    if (pendingTask != null) {
      lock.lock();
      try {
        if (pendingTask != null) {
          resumeTask = pendingTask;
          pendingTask = null;

          resumeItem = pendingItem;
          pendingItem = null.asInstanceOf[I]

          resumeCallback = pendingCallback;
          pendingCallback = null;
        }
      } finally {
        lock.unlock();
      }

      if (resumeTask != null) {
        if (err == null) {
          resumeTask(taskCallback, resumeItem);
          resumeCallback.completed()
        } else {
          // TODO Catch exception
          runningCount.decrementAndGet();
          resumeCallback.error(err);
        }
      }
    }
  }
}

class Do[R](body: (Callback[R]) => Unit) {
  def _while(asyncCond: (Callback[Boolean], R) => Unit) = new DoWhile(body, asyncCond)
  def _while(cond: (R) => Boolean): DoWhile[R] = {
    _while((k: Callback[Boolean], r: R) =>
      try { k.completed(cond(r)) } catch { case t => k.error(t) })
  }
}
class UntilGenerator[R](pk: Callback[Unit], cond: (Callback[Boolean], R) => Unit) {
  def foreach(task: (Callback[R]) => Unit) = {
    try {
      new DoWhile(task, cond).run(pk)
    } catch { case e => pk.error(e) }
  }
  def filter(x: (Callback[R]) => Unit) = this
}

class DoWhile[R](body: (Callback[R]) => Unit, cond: (Callback[Boolean], R) => Unit) {
  def run(k: Callback[Unit]) {
    try {
      body(new BodyCallback(k))
    } catch { case t => k.error(t) }
  }

  class BodyCallback(pk: Callback[Unit]) extends Callback[R] {
    val condK: Callback[Boolean] = new ConditionCallback(pk, this)
    def completed(r: R) {
      cond(condK, r)
    }
    def error(e: Throwable) {
      pk.error(e)
    }
  }

  class ConditionCallback(pk: Callback[Unit], k: Callback[R]) extends Callback[Boolean] {
    def completed(b: Boolean) {
      try {
        b match {
          case true => body(k)
          case false => pk.completed()
        }
      } catch { case t => pk.error(t) }
    }

    def error(e: Throwable) {
      pk.error(e)
    }
  }
}

class PipeGenerator[R, S](pk: Callback[S], task: (Callback[R]) => Unit) {
  def foreach(andThen: ((Callback[S], R)) => Unit) = try {
    task(new CallbackBase[R, S](pk) {
      def completed(r: R) = try {
        andThen((pk, r))
      } catch { case e => pk.error(e) }
    })
  } catch { case e => pk.error(e) }

  def filter(x: ((Callback[S], R)) => Unit) = this
}

class RetryCallback[R](pk: Callback[R], task: (Callback[R]) => Unit, count: Int) extends Callback[R] {
  def completed(r: R) = try {
    pk.completed(r)
  } catch { case e => pk.error(e) }

  def error(e: Throwable) = try {
    count match {
      case 0 => pk.error(e)
      case _ => {
        println("Retrying on " + e.getClass().getSimpleName() + ": " + e.getMessage())
        task(new RetryCallback(pk, task, count - 1))
      }
    }
  } catch { case e => pk.error(e) }
}

class RetryGenerator[R](pk: Callback[R], retryCount: Int) {
  def foreach(task: (Callback[R]) => Unit) = try {
    task(new RetryCallback[R](pk, task, retryCount))
  } catch { case e => pk.error(e) }

  def filter(x: (Callback[R]) => Unit) = this
}
