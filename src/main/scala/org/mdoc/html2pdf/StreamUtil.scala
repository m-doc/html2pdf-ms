package org.mdoc.html2pdf

import scalaz.Bind
import scalaz.stream._
import scalaz.stream.Process._
import scalaz.syntax.bind._

object StreamUtil {
  implicit class MyChannelSyntax[F[_], I, O](val self: Channel[F, I, O]) extends AnyVal {
    def andThen[O2](f: O => F[O2])(implicit F: Bind[F]): Channel[F, I, O2] =
      self.map(g => g.andThen(_.flatMap(f)))

    def compose[I0](f: I0 => F[I])(implicit F: Bind[F]): Channel[F, I0, O] =
      self.map(g => f.andThen(_.flatMap(g)))
  }

  implicit class MySinkSyntax[F[_], O](val self: Sink[F, O]) extends AnyVal {
    def combine(other: Sink[F, O])(implicit F: Bind[F]): Sink[F, O] =
      self.zipWith(other)((f, g) => o => f(o) >> g(o))
  }

  implicit class MyWriterSyntax[F[_], W, O](val self: Writer[F, W, O]) extends AnyVal {
    def ignoreO: Writer[F, W, Nothing] =
      self.flatMapO(_ => halt)
  }

  def evalO[F[_], O](f: F[O]): Writer[F, Nothing, O] =
    await(f)(emitO)

  def runIf[F[_], O](b: Boolean)(p: => Process[F, O]): Process[F, O] =
    if (b) p else halt
}