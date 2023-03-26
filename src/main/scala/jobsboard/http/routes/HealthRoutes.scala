package com.github.dpratt747
package jobsboard.http.routes

import cats.*
import cats.effect.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.*
import org.http4s.dsl.*
import org.typelevel.log4cats.Logger

trait HealthRoutesAlg[F[_]] {
  def routes: HttpRoutes[F]
}

final case class HealthRoutes[F[_] : Concurrent: Logger] private() extends HealthRoutesAlg[F] with Http4sDsl[F]{
  private def healthRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok("Ok")
  }

  override def routes: HttpRoutes[F] = Router(
    "/health" -> healthRoute
  )

}

object HealthRoutes {
  def make[F[_] : Concurrent: Logger](): HealthRoutesAlg[F] = HealthRoutes[F]()
}
