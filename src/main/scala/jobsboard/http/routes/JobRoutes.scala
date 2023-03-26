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

trait JobRoutesAlg[F[_]] {
  def routes: HttpRoutes[F]
}

final case class JobRoutes[F[_] : Concurrent: Logger] private() extends JobRoutesAlg[F] with Http4sDsl[F] {

  private val database: mutable.Map[JobId, Job] = mutable.Map[JobId, Job]()

  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    Ok(database.values)
  }

  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    database.get(JobId(id)) match {
      case Some(job) => Ok(job)
      case None => NotFound(FailureResponse(s"Job with id: $id not found"))
    }
  }

  private def createJob(jobInfo: JobInfo): F[Job] =
    Job(
      id = JobId(java.util.UUID.randomUUID()),
      date = System.currentTimeMillis(),
      ownerEmail = Email("todo@gmail.com"),
      jobInfo = jobInfo
    ).pure[F]

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req@POST -> Root / "create" =>
    for {
      _ <- Logger[F].info(s"Attempting to create job")
      jobInfo <- req.as[JobInfo].logError(e => s"Parsing failed: ${e.getMessage}")  // poor error response like this
      _ <- Logger[F].info(s"Parsed request body, Job info: $jobInfo")
      job <- createJob(jobInfo)
      _ <- database.put(job.id, job).pure[F]
      _ <- Logger[F].info(s"Created job: $job")
      resp <- Created(job.id)
    } yield resp
  }

  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / "update" / UUIDVar(id) =>
      database.get(JobId(id)) match {
        case Some(job) =>
          for {
            ji <- req.as[JobInfo]
            updatedJob = job.copy(jobInfo = ji)
            _ <- database.put(updatedJob.id, updatedJob).pure[F]
            resp <- Ok(updatedJob)
          } yield resp
        case None => NotFound(FailureResponse(s"Job with id: $id not found. Cannot update"))
      }
  }

  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / UUIDVar(id) =>
    database.remove(JobId(id)) match {
      case Some(_) => Ok()
      case None => NotFound(FailureResponse(s"Job with id: $id not found. Cannot delete"))
    }
  }

  override def routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )

}

object JobRoutes {
  def make[F[_] : Concurrent: Logger](): JobRoutesAlg[F] = JobRoutes[F]()

}
