package com.github.dpratt747
package jobsboard.core.program

import jobsboard.core.repository.JobsRepositoryAlg
import jobsboard.domain.job.*
import jobsboard.domain.job.Email.*
import jobsboard.domain.job.JobId.*
import jobsboard.domain.pagination.Pagination

import cats.implicits.*
import cats.{Applicative, Monad}
import org.typelevel.log4cats.Logger

trait JobsProgramAlg[F[_]] {
  def insertJob(ownerEmail: Email, jobsInfo: JobInfo): F[JobId]

  def getAll(filter: JobFilter, pagination: Pagination): F[List[Job]]

  def getAll(): F[List[Job]]

  def findByJobId(jobId: JobId): F[Option[Job]]

  def updateJob(jobId: JobId, jobInfo: JobInfo): F[Option[Job]]

  def delete(jobId: JobId): F[Int]
}

final case class JobsProgram[F[_]: Logger: Monad] private (
    private val jobsRepositoryAlg: JobsRepositoryAlg[F]
) extends JobsProgramAlg[F] {
  override def insertJob(ownerEmail: Email, jobsInfo: JobInfo): F[JobId] =
    for {
      _     <- Logger[F].info(s"Attempting to create job")
      jobId <- jobsRepositoryAlg.createJob(ownerEmail, jobsInfo)
    } yield jobId

  override def getAll(filter: JobFilter, pagination: Pagination): F[List[Job]] = {
    for {
      _    <- Logger[F].info(s"Attempting to get all jobs")
      jobs <- jobsRepositoryAlg.all(filter, pagination)
    } yield jobs
  }

  override def getAll(): F[List[Job]] =
    for {
      _    <- Logger[F].info(s"Attempting to get all jobs")
      jobs <- jobsRepositoryAlg.all()
    } yield jobs

  override def findByJobId(jobId: JobId): F[Option[Job]] =
    for {
      _     <- Logger[F].info(s"Attempting to get job with id: $jobId")
      jobId <- jobsRepositoryAlg.find(jobId)
    } yield jobId

  override def updateJob(jobId: JobId, jobInfo: JobInfo): F[Option[Job]] =
    for {
      _    <- Logger[F].info(s"Attempting to update job with id: $jobId")
      jobO <- jobsRepositoryAlg.update(jobId, jobInfo)
    } yield jobO

  override def delete(jobId: JobId): F[Int] =
    for {
      _        <- Logger[F].info(s"Attempting to delete job with id: $jobId")
      rowCount <- jobsRepositoryAlg.delete(jobId)
    } yield rowCount

}

object JobsProgram {
  def make[F[_]: Monad: Logger](repo: JobsRepositoryAlg[F]): F[JobsProgramAlg[F]] = JobsProgram(
    repo
  ).pure[F]
}
