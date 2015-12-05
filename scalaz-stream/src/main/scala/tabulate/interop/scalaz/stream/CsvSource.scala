package tabulate.interop.scalaz.stream

import simulacrum.{noop, op, typeclass}
import tabulate.{CsvData, CsvInput, DecodeResult, RowDecoder}

import scalaz.concurrent.Task
import scalaz.stream._

/** Turns instances of `S` into CSV sources.
  *
  * Any type `S` that has a implicit instance of `CsvSource` in scope will be enriched by the `asCsvSource` and
  * `asUnsafeCsvSource` methods (which map to [[source]] and [[unsafeSource]] respectively).
  *
  * Additionally, any type that has an instance of `CsvInput` in scope automatically gets an instance of `CsvSource`.
  */
@typeclass trait CsvSource[S] {
  @noop def toCsvData(s: S): CsvData

  @op("asCsvSource") def source[A: RowDecoder](s: S, sep: Char, header: Boolean): Process[Task, DecodeResult[A]] =
    io.iteratorR(Task.delay(toCsvData(s)))(src => Task.delay(src.close()))(src => Task.delay(src.asRows[A](sep, header).toIterator))

  @op("asUnsafeCsvSource") def unsafeSource[A: RowDecoder](s: S, sep: Char, header: Boolean): Process[Task, A] =
    source(s, sep, header).map(_.get)
}

object CsvSource {
  implicit def fromInput[S: CsvInput]: CsvSource[S] = new CsvSource[S] {
    override def toCsvData(s: S) = CsvInput[S].toCsvData(s)
  }
}
