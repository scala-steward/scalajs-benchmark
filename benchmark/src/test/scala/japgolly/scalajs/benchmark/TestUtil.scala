package japgolly.scalajs.benchmark

import japgolly.scalajs.benchmark.engine._
import scala.concurrent.duration._

object TestUtil extends japgolly.microlibs.testutil.TestUtil {

  implicit def durationToMs(d: Duration): Double =
    TimeUtil.toMs(d)

  implicit def msToDuration(d: Double): Duration =
    TimeUtil.fromMs(d)

  def stats(durs: Duration*): Stats =
    Stats(durs.iterator.map(IterationStats(1, _)).toVector)

  def statPlusMinus(dur: FiniteDuration, pm: FiniteDuration) =
    stats(dur, dur + pm, dur - pm)

  def statMatrix(iterations: Int, opsPerIteration: Int)(f: (Int, Int) => Duration): Stats =
    Stats(Vector.tabulate(iterations) { i =>
      val sum = (1 to opsPerIteration).iterator.map(f(i, _)).map(TimeUtil.toMs).sum
      IterationStats(opsPerIteration, sum)
    })

  def itStats(times: Double*): IterationStats = {
    val b = new IterationStats.Builder
    times.foreach(b.add)
    b.result()
  }

  def stats(i1: IterationStats, in: IterationStats*): Stats =
    Stats(i1 +: in.toVector)

}
