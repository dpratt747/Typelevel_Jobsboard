package com.github.dpratt747
package jobsboard

import cats.MonadError
import cats.implicits.*
import org.typelevel.log4cats.*

package object logging {
  extension[F[_], E, A](fa: F[A])(using me: MonadError[F, E], logger: Logger[F]) {
    def log(success: A => String, failure: E => String): F[A] = fa.attemptTap {
      case Left(e)  => logger.error(failure(e))
      case Right(a) => logger.info(success(a))
    }

    def logError(failure: E => String): F[A] = fa.attemptTap {
      case Left(e)  => logger.error(failure(e))
      case Right(_) => ().pure[F]
    }

  }
}
