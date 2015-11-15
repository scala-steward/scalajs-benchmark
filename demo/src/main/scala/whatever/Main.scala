package whatever

import japgolly.scalajs.react._, vdom.prefix_<^._, MonocleReact._
import whatever.ReactChart.ScalaDataset
import whatever.chartjs.Chart
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit._
import scalajs.js
import org.scalajs.dom
import monocle.macros._
import monocle._
import scalacss.Defaults._
import scalacss.ScalaCssReact._

import Benchy._

object Main extends js.JSApp {
  def main(): Unit = {

    val s = IntSet_X.suite
//    val s = IntSet_X2.suite

//        runToConsole(s)

    chartjs.Chart.defaults.global.animationSteps = 20

    // Ensure benchmarks don't start before chart animation finishes
    Benchy.minBmDelay = {
      val chartTimeSec = chartjs.Chart.defaults.global.animationSteps / 60.0
      val delaySec = chartTimeSec * 1.2
      val delayMicro = delaySec * 1000000.0
      delayMicro.toInt.micros
    }

    Styles.addToDocument()
    val tgt = dom.document.getElementById("body")
    ReactDOM.render(BMComp.Comp(s), tgt)
  }
}
