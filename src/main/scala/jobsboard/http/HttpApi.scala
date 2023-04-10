package com.github.dpratt747
package jobsboard.http

import jobsboard.http.routes.*

import cats.*
import cats.implicits.*
import cats.effect.*
import com.github.dpratt747.jobsboard.config.ApplicationConfig
import com.github.dpratt747.jobsboard.core.program.{AuthProgramAlg, JobsProgram, JobsProgramAlg}
import com.github.dpratt747.jobsboard.domain.security.Authenticator
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

trait HttpApiAlg[F[_]] {
  def routes: F[HttpRoutes[F]]
}

final case class HttpApi[F[_]: Concurrent: Logger] private (
    private val jobsProgram: JobsProgramAlg[F],
    private val authProgram: AuthProgramAlg[F],
    private val authenticator: Authenticator[F]
) extends HttpApiAlg[F] {

  override def routes: F[HttpRoutes[F]] = for {
    healthRoutes <- HealthRoutes.make[F]()
    authRoutes   <- AuthRoutes.make[F](authProgram, authenticator)
    jobRoutes    <- JobRoutes.make[F](jobsProgram, authenticator)
  } yield Router(
    "/api" -> (healthRoutes.routes <+> jobRoutes.routes <+> authRoutes.routes)
  )

}

object HttpApi {
  def make[F[_]: Concurrent: Logger](
      jobsProgram: JobsProgramAlg[F],
      authProgram: AuthProgramAlg[F],
      authenticator: Authenticator[F]
  ): F[HttpApiAlg[F]] = HttpApi[F](jobsProgram, authProgram, authenticator).pure[F]
}
