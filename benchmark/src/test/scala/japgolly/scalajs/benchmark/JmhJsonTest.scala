package japgolly.scalajs.benchmark

import io.circe._
import io.circe.parser._
import japgolly.microlibs.testutil.TestUtil._
import japgolly.scalajs.benchmark.engine._
import japgolly.scalajs.benchmark.gui.{BMDone, BMStatus, FormatResult, GuiParam, GuiParams, GuiSuite}
import japgolly.scalajs.benchmark.gui.FormatResults.{Args, JmhJson}
import monocle.Iso
import scala.concurrent.duration._
import scala.scalajs.js
import utest._

object JmhJsonTest extends TestSuite {
  override def tests = Tests {
    "simple" - testSimple()
    "params" - testWithParams()
  }

  private val eo = EngineOptions.default.copy(
    warmupIterationTime = None,
    warmupIterations    = 1,
    iterations          = 4,
    iterationTime       = 2.seconds,
  )

  private val startTime =
    new js.Date()

  private def assertEqJson(actual: Json, expect: String): Unit = {
    val e = parse(expect).getOrThrow()
    assertEqJson(actual, e)
  }

  private def assertEqJson(actual: Json, expect: Json): Unit = {
    val a = actual.spaces2SortKeys
    val e = expect.spaces2SortKeys
    assertMultiline(a, e)
  }

  private def testJmhJson[P](suite     : GuiSuite[P],
                             progress  : Progress[P],
                             results   : Map[PlanKey[P], BMStatus],
                             resultFmts: Vector[FormatResult],
                             expect    : String): Unit = {
    val args = Args[P](suite, progress, results, resultFmts)
    val actual = JmhJson.json(args).mapArray(_.map(_.mapObject(_.filterKeys(_ != "userAgent"))))
    assertEqJson(actual, expect)
  }

  private def testSimple() = {
    val bm1    = Benchmark("My BM")(())
    val suite  = Suite[Unit]("My Suite")(bm1)
    val plan   = Plan[Unit](suite, Vector.empty)
    val bm1p0  = PlanKey[Unit](0, 0)(bm1, ())
    val bm1p0r = Vector(Vector(12.1.millis, 12.2.millis), Vector(12.3.millis, 12.2.millis))

    val expect =
      s"""[
        |  {
        |    "benchmark": "My_Suite.My_BM",
        |    "mode": "avgt",
        |    "threads": 1,
        |    "forks": 1,
        |    "jdkVersion": "1.8",
        |    "vmName": "Scala.JS",
        |    "vmVersion": "${ScalaJsInfo.version}",
        |    "warmupIterations": 1,
        |    "warmupTime": "2 s",
        |    "warmupBatchSize": 1,
        |    "measurementIterations": 4,
        |    "measurementTime": "2 s",
        |    "measurementBatchSize": 1,
        |    "primaryMetric": {
        |      "score": 12.2,
        |      "scoreError": 0.527619,
        |      "scoreConfidence": [
        |        11.67238,
        |        12.727619
        |      ],
        |      "scoreUnit": "ms/op",
        |      "rawData": [
        |        [
        |          12.15,
        |          12.25
        |        ]
        |      ]
        |    },
        |    "secondaryMetrics": {
        |    }
        |  }
        |]
        |""".stripMargin

    testJmhJson[Unit](
      suite      = GuiSuite(suite),
      progress   = Progress(startTime, plan, 123),
      results    = Map(bm1p0 -> BMDone(Right(Stats(bm1p0r, eo)))),
      resultFmts = Vector(FormatResult.MillisPerOp),
      expect     = expect,
    )
  }

  private def testWithParams() = {
    type P     = (Int, Boolean)
    val p1     = (123, true)
    val p2     = (654, false)
    val gp1    = GuiParam.int("Size", 5, 10)
    val gp2    = GuiParam.boolean("On")
    val gps    = GuiParams.two(Iso.id[P], gp1, gp2)
    val bm1    = Benchmark[P]("bm1", _ => ())
    val suite  = Suite[P]("Suite")(bm1)
    val plan   = Plan[P](suite, Vector(p1, p2))
    val bm1p1  = PlanKey[P](0, 0)(bm1, p1)
    val bm1p2  = PlanKey[P](0, 1)(bm1, p2)
    val bm1p1r = Vector(Vector(10.micros, 9.micros), Vector(9.micros, 10.micros))
    val bm1p2r = Vector(Vector(13.micros, 13.micros), Vector(13.micros, 13.micros))

    val expect =
      s"""[
        |  {
        |    "benchmark": "Suite.bm1",
        |    "mode": "avgt",
        |    "threads": 1,
        |    "forks": 1,
        |    "jdkVersion": "1.8",
        |    "vmName": "Scala.JS",
        |    "vmVersion": "${ScalaJsInfo.version}",
        |    "warmupIterations": 1,
        |    "warmupTime": "2 s",
        |    "warmupBatchSize": 1,
        |    "measurementIterations": 4,
        |    "measurementTime": "2 s",
        |    "measurementBatchSize": 1,
        |    "params": {
        |      "Size": "123",
        |      "On": "T"
        |    },
        |    "primaryMetric": {
        |      "score": 9.5,
        |      "scoreError": 3.73,
        |      "scoreConfidence": [
        |        5.769,
        |        13.23
        |      ],
        |      "scoreUnit": "us/op",
        |      "rawData": [
        |        [
        |          9.5,
        |          9.5
        |        ]
        |      ]
        |    },
        |    "secondaryMetrics": {
        |    }
        |  },
        |  {
        |    "benchmark": "Suite.bm1",
        |    "mode": "avgt",
        |    "threads": 1,
        |    "forks": 1,
        |    "jdkVersion": "1.8",
        |    "vmName": "Scala.JS",
        |    "vmVersion": "${ScalaJsInfo.version}",
        |    "warmupIterations": 1,
        |    "warmupTime": "2 s",
        |    "warmupBatchSize": 1,
        |    "measurementIterations": 4,
        |    "measurementTime": "2 s",
        |    "measurementBatchSize": 1,
        |    "params": {
        |      "Size": "654",
        |      "On": "F"
        |    },
        |    "primaryMetric": {
        |      "score": 13,
        |      "scoreError": 0,
        |      "scoreConfidence": [
        |        13,
        |        13
        |      ],
        |      "scoreUnit": "us/op",
        |      "rawData": [
        |        [
        |          13,
        |          13
        |        ]
        |      ]
        |    },
        |    "secondaryMetrics": {
        |    }
        |  }
        |]
        |""".stripMargin

    testJmhJson[P](
      suite      = GuiSuite(suite, gps),
      progress   = Progress(startTime, plan, 123),
      results    = Map(bm1p1 -> BMDone(Right(Stats(bm1p1r, eo))), bm1p2 -> BMDone(Right(Stats(bm1p2r, eo)))),
      resultFmts = Vector(FormatResult.MicrosPerOp, FormatResult.OpsPerSec),
      expect     = expect,
    )
  }
}
