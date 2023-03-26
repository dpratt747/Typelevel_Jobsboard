package com.github.dpratt747
package jobsboard.http

import jobsboard.http.routes.*

import cats.*
import cats.implicits.*
import cats.effect.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

trait HttpApiAlg[F[_]] {
  def routes: HttpRoutes[F]
}

final case class HttpApi[F[_]: Concurrent: Logger] private() extends HttpApiAlg[F] {
  private val healthRoutes = HealthRoutes.make[F]().routes
  private val jobRoutes = JobRoutes.make[F]().routes

  override def routes: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )

}

object HttpApi {
  def make[F[_]: Concurrent: Logger](): HttpApiAlg[F] = HttpApi[F]()
}
