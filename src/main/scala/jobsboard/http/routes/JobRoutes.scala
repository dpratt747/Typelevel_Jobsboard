package com.github.dpratt747
package jobsboard.http.routes

import jobsboard.core.program.{JobsProgram, JobsProgramAlg}
import jobsboard.domain.job.JobId.*
import jobsboard.domain.pagination.*
import jobsboard.domain.job.{JobId, *}
import jobsboard.http.responses.*
import jobsboard.logging.*

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

trait JobRoutesAlg[F[_]] {
  def routes: HttpRoutes[F]
}

final case class JobRoutes[F[_] : Concurrent : Logger] private(
                                                                private val jobsProgram: JobsProgramAlg[F]
                                                              ) extends JobRoutesAlg[F] with Http4sDsl[F] {

  private object OffsetQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Long]("offset")

  private object LimitQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Long]("limit")

  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req@POST -> Root :? LimitQueryParamMatcher(limit) +& OffsetQueryParamMatcher(offset) =>
    req.attemptAs[JobFilter].value.flatMap {
      case Right(filter) =>
        for {
          _ <- Logger[F].info("Getting all jobs")
          jobs <- jobsProgram.getAll(filter, Pagination(limit, offset))
          resp <- Ok(jobs)
        } yield resp
      case Left(e) => InternalServerError(FailureResponse(e.getMessage))
    }
  }

  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    Logger[F].info(s"Getting job with id: $id") *>
      jobsProgram.findByJobId(JobId(id)).flatMap {
        case Some(job) => Ok(job)
        case None => NotFound(FailureResponse(s"Job with id: $id not found"))
      }
  }

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req@POST -> Root / "create" =>
    req.attemptAs[JobInfo].value.flatMap {
      case Right(jobInfo) =>
        for {
          _ <- Logger[F].info(s"Attempting to create job")
          id <- jobsProgram.insertJob(Email("todo@mail.com"), jobInfo)
          resp <- Created(id)
        } yield resp
      case Left(e) => UnprocessableEntity(FailureResponse(e.getMessage))
    }
  }

  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req@PUT -> Root / "update" / UUIDVar(id) =>
    req.attemptAs[JobInfo].value.flatMap {
      case Right(jobInfo) =>
        for {
          _ <- Logger[F].info(s"Attempting to update job")
          update <- jobsProgram.updateJob(JobId(id), jobInfo)
          resp <- update match {
            case Some(job) => Ok(job)
            case None => NotFound(FailureResponse(s"Job with id: $id not found. Cannot update"))
          }
        } yield resp
      case Left(e) => InternalServerError(FailureResponse(e.getMessage))
    }
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
