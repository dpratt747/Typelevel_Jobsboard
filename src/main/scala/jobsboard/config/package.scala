package com.github.dpratt747
package jobsboard

import pureconfig.*
import cats.implicits.*
import cats.*
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag

package object config {
  extension (source: ConfigSource) {
    def loadF[F[_], A](using reader: ConfigReader[A], F: MonadThrow[F], tag: ClassTag[A]): F[A] =
      F.pure(source.load[A]).flatMap {
        case Left(errors) => F.raiseError[A](ConfigReaderException(errors))
        case Right(value) => value.pure[F]
      }
  }
}
