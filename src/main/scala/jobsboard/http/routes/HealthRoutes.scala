package com.github.dpratt747
package jobsboard.http.routes

import cats.*
import cats.implicits.*
import cats.effect.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.*
import org.http4s.dsl.*
import org.typelevel.log4cats.Logger

trait HealthRoutesAlg[F[_]] {
  def routes: HttpRoutes[F]
}

final case class HealthRoutes[F[_]: Concurrent: Logger] private ()
    extends HealthRoutesAlg[F]
    with Http4sDsl[F] {
  private val healthRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Logger[F].info("Health check") *>
      Ok("Ok")
  }

  override def routes: HttpRoutes[F] = Router(
    "/health" -> healthRoute
  )

}

object HealthRoutes {
  def make[F[_]: Concurrent: Monad: Logger](): F[HealthRoutesAlg[F]] = HealthRoutes[F]().pure[F]
}
