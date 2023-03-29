package com.github.dpratt747
package jobsboard.http.routes

import jobsboard.domain.job.JobId.*
import jobsboard.domain.job.{JobId, *}
import jobsboard.http.responses.*

import cats.*
import cats.effect.*
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.parser.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.collection.mutable
import jobsboard.logging.*

import com.github.dpratt747.jobsboard.core.program.{JobsProgram, JobsProgramAlg}

trait JobRoutesAlg[F[_]] {
  def routes: HttpRoutes[F]
}

final case class JobRoutes[F[_] : Concurrent : Logger] private(
                                                                private val jobsProgram: JobsProgramAlg[F]
                                                              ) extends JobRoutesAlg[F] with Http4sDsl[F] {


  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    Logger[F].info("Getting all jobs") *> jobsProgram.getAll().flatMap(Ok(_))
  }

  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    Logger[F].info(s"Getting job with id: $id") *>
      jobsProgram.findByJobId(JobId(id)).flatMap {
        case Some(job) => Ok(job)
        case None => NotFound(FailureResponse(s"Job with id: $id not found"))
      }
  }

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req@POST -> Root / "create" =>
    for {
      _ <- Logger[F].info(s"Attempting to create job")
      jobInfo <- req.as[JobInfo].logError(e => s"Parsing failed: ${e.getMessage}") // poor error response like this
      _ <- Logger[F].info(s"Parsed request body, Job info: $jobInfo")
      id <- jobsProgram.insertJob(Email("todo@mail.com"), jobInfo)
      resp <- Created(id)
    } yield resp
  }

  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@PUT -> Root / "update" / UUIDVar(id) =>
      for {
        _ <- Logger[F].info(s"Attempting to update job")
        ji <- req.as[JobInfo]
        _ <- Logger[F].info(s"Parsed request body, Job info: $ji")
        job <- jobsProgram.updateJob(JobId(id), ji)
        res <- job match {
          case Some(job) => Ok(job)
          case None => NotFound(FailureResponse(s"Job with id: $id not found. Cannot update"))
        }
      } yield res
  }

  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / UUIDVar(id) =>
    val jobId = JobId(id)
    Logger[F].info(s"Attempting to delete job with id: $id") *>
    jobsProgram.findByJobId(jobId).flatMap {
      case Some(_) => jobsProgram.delete(jobId).flatMap(Ok(_))
      case None => NotFound(FailureResponse(s"Job with id: $id not found. Cannot delete"))
    }
  }

  override def routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )

}

object JobRoutes {
  def make[F[_] : Concurrent : Monad : Logger](jobsProgram: JobsProgramAlg[F]): F[JobRoutesAlg[F]] = JobRoutes[F](jobsProgram).pure[F]

}
