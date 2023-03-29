package com.github.dpratt747
package jobsboard.http

import jobsboard.http.routes.*

import cats.*
import cats.implicits.*
import cats.effect.*
import com.github.dpratt747.jobsboard.core.program.{JobsProgram, JobsProgramAlg}
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

trait HttpApiAlg[F[_]] {
  def routes: F[HttpRoutes[F]]
}

final case class HttpApi[F[_] : Concurrent : Logger] private(
                                                              private val jobsProgram: JobsProgramAlg[F]
                                                            ) extends HttpApiAlg[F] {

  override def routes: F[HttpRoutes[F]] = for {
    healthRoutes <- HealthRoutes.make[F]()
    jobRoutes <- JobRoutes.make[F](jobsProgram)
  } yield Router(
    "/api" -> (healthRoutes.routes <+> jobRoutes.routes)
  )

}

object HttpApi {
  def make[F[_] : Concurrent : Logger](jobsProgram: JobsProgramAlg[F]): F[HttpApiAlg[F]] = HttpApi[F](jobsProgram).pure[F]
}
