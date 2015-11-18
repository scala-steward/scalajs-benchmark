package japgolly.scalajs.benchmark.gui

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.{Iso, Lens}
import scalaz.{\/-, \/}
import scalaz.std.option.optionSyntax._
import Param.{Editor, Header}
import Params._

trait Params[P] {

  def initialState: GenState

  def parseState(s: GenState): ParseResult[P]

  def headers: Vector[Header]

  def editors: Vector[GenEditor]

  def renderParams(p: P): Vector[TagMod]
}

object Params {
  type GenState = Vector[Any]
  type GenEditor = Editor[GenState]
  type ParseResult[P] = Header \/ Vector[P]

  import Internals._

  val none: Params[Unit] =
    new Params[Unit] {
      override def initialState            = Vector.empty
      override def headers                 = Vector.empty
      override def editors                 = Vector.empty
      override def renderParams(p: Unit)   = Vector.empty
      override def parseState(s: GenState) = parseResult
      val parseResult = \/-(Vector(()))
    }

  def one[P, E](param: Param[P, E]): Params[P] = {
    val p = SubParam(0, param, Lens.id[P])
    val ps = Vector(p)

    new MostlyGenericParams(ps) {
      override def parseState(s: GenState): ParseResult[P] =
        p.parse(p.key.get(s)) \/> p.param.header
    }
  }

  def two[P, P1, E1, P2, E2](iso: Iso[P, (P1, P2)], param1: Param[P1, E1], param2: Param[P2, E2]): Params[P] = {
    import monocle.function.{first, second}
    import monocle.std.tuple2._

    val p1 = SubParam(0, param1, iso ^|-> first)
    val p2 = SubParam(1, param2, iso ^|-> second)
    val ps = Vector(p1, p2)

    new MostlyGenericParams(ps) {
      override def parseState(s: GenState): ParseResult[P] =
        for {
          v1 <- p1.parse(p1.key.get(s)) \/> p1.param.header
          v2 <- p2.parse(p2.key.get(s)) \/> p2.param.header
        } yield
          for {a1 <- v1; a2 <- v2} yield iso.reverseGet((a1, a2))
    }
  }

  // ===================================================================================================================
  private[this] object Internals {

    def emptyState(size: Int): GenState =
      Vector.fill(size)(())

    type Key[T] = Lens[GenState, T]

    def Key[T](stateIndex: Int): Key[T] =
      Lens[GenState, T](_.apply(stateIndex).asInstanceOf[T])(v => _.updated(stateIndex, v))

    trait SubParam[P] {
      type A
      type B
      val lens: Lens[P, A]
      val param: Param[A, B]
      val key: Key[B]

      val editor: GenEditor =
        e => param editor ExternalVar(key get e.value)(b => e.set(key.set(b)(e.value)))

      def parse(b: B): Option[Vector[A]] =
        param.parser.getOption(b)
    }

    object SubParam {
      type Aux[P, X, Y] = SubParam[P] {type A = X; type B = Y}

      def apply[P, X, Y](stateIndex: Int, p: Param[X, Y], _lens: Lens[P, X]): Aux[P, X, Y] =
        new SubParam[P] {
          override type A = X
          override type B = Y
          override val param = p
          override val key = Key[B](stateIndex)
          override val lens = _lens
        }
    }

    /**
      * MostlyGeneric cos I'm tired and I suck.
      *
      * TODO Use a HList
      */
    abstract class MostlyGenericParams[P](ps: Vector[SubParam[P]]) extends Params[P] {
      override final def initialState: GenState =
        ps.foldLeft(emptyState(ps.length))((m, p) =>
          p.key.set(p.param.parser reverseGet p.param.initValues)(m))

      override final val headers: Vector[Header] =
        ps.map(_.param.header)

      override final val editors: Vector[GenEditor] =
        ps.map(_.editor)

      override final def renderParams(i: P): Vector[TagMod] =
        ps.map(p => p.param render p.lens.get(i))
    }
  }
}
