package com.github.dpratt747
package jobsboard.core.repository

import jobsboard.domain.job.Country.Country
import jobsboard.domain.job.Email.Email
import jobsboard.domain.job.JobId.JobId
import jobsboard.domain.job.CompanyName.*
import jobsboard.domain.job.*

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import cats.Applicative
import cats.effect.kernel.MonadCancelThrow
import cats.implicits.*
import org.typelevel.log4cats.Logger
import jobsboard.domain.pagination.*
import jobsboard.logging.*

trait JobsRepositoryAlg[F[_]] {
  def createJob(ownerEmail: Email, jobInfo: JobInfo): F[JobId]

  def all(): F[List[Job]]

  def all(filter: JobFilter, pagination: Pagination): F[List[Job]]

  def find(jobId: JobId): F[Option[Job]]

  def update(jobId: JobId, jobInfo: JobInfo): F[Option[Job]]

  def delete(jobId: JobId): F[Int]
}

final case class JobsRepository[F[_]: MonadCancelThrow: Logger] private (
    private val xa: Transactor[F]
) extends JobsRepositoryAlg[F] {
  override def createJob(ownerEmail: Email, jobInfo: JobInfo): F[JobId] =
    sql"""
         |INSERT INTO jobs (
         |  date, owner_email, company, title, description, external_url, remote, location, currency, salary_lo,
         |  salary_hi, country, tags, image, seniority, other, active
         |) VALUES (
         |  ${System
          .currentTimeMillis()}, $ownerEmail, ${jobInfo.company}, ${jobInfo.title}, ${jobInfo.description},
         |  ${jobInfo.externalUrl}, ${jobInfo.remote}, ${jobInfo.location}, ${jobInfo.currency}, ${jobInfo.salaryLo},
         |  ${jobInfo.salaryHi}, ${jobInfo.country}, ${jobInfo.tags}, ${jobInfo.image}, ${jobInfo.seniority},
         |  ${jobInfo.other}, false
         |)
         |""".stripMargin.update
      .withUniqueGeneratedKeys[JobId]("id")
      .transact(xa)

  override def all(): F[List[Job]] =
    sql"""
         |SELECT
         |  id, date, owner_email, company, title, description, external_url, remote, location, currency, salary_lo,
         |  salary_hi, country, tags, image, seniority, other, active
         |FROM jobs
         |""".stripMargin
      .query[Job]
      .to[List]
      .transact(xa)

  override def all(filter: JobFilter, pagination: Pagination): F[List[Job]] = {
    val selectFragment: Fragment =
      fr"""
          |SELECT
          |  id, date, owner_email, company, title, description, external_url, remote, location, currency, salary_lo,
          |  salary_hi, country, tags, image, seniority, other, active
          |""".stripMargin

    val fromFragment: Fragment = fr"FROM jobs"

    val whereFragment: Fragment = Fragments.whereAndOpt(
      filter.companies.flatMap(_.toNel).map(list => Fragments.in(fr"company", list)),
      filter.locations.flatMap(_.toNel).map(list => Fragments.in(fr"location", list)),
      filter.tags
        .flatMap(_.toNel)
        .map(list => Fragments.or(list.map(tag => fr"$tag = any(tags)").toList: _*)),
      filter.countries.flatMap(_.toNel).map(list => Fragments.in(fr"country", list)),
      filter.seniorities.flatMap(_.toNel).map(list => Fragments.in(fr"seniority", list)),
      filter.maxSalary.map(salaryHigh => fr"salary_hi > $salaryHigh"),
      filter.remote.map(remote => fr"remote = $remote")
    )

    val paginationFragment = fr"ORDER BY id LIMIT ${pagination.limit} OFFSET ${pagination.offset}"

    val statement = selectFragment |+| fromFragment |+| whereFragment |+| paginationFragment

    Logger[F].info(statement.toString) *>
      statement
        .query[Job]
        .to[List]
        .transact(xa)
        .logError(e => s"Failed to get jobs: ${e.getMessage}")
  }

  override def find(jobId: JobId): F[Option[Job]] =
    sql"""
         |SELECT
         |  id, date, owner_email, company, title, description, external_url, remote, location, currency, salary_lo,
         |  salary_hi, country, tags, image, seniority, other, active
         |FROM jobs
         |WHERE id = $jobId
         |""".stripMargin
      .query[Job]
      .option
      .transact(xa)

  override def update(jobId: JobId, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
         |UPDATE jobs
         |SET
         |  company = ${jobInfo.company},
         |  title = ${jobInfo.title},
         |  description = ${jobInfo.description},
         |  external_url = ${jobInfo.externalUrl},
         |  remote = ${jobInfo.remote},
         |  location = ${jobInfo.location},
         |  currency = ${jobInfo.currency},
         |  salary_lo = ${jobInfo.salaryLo},
         |  salary_hi = ${jobInfo.salaryHi},
         |  country = ${jobInfo.country},
         |  tags = ${jobInfo.tags},
         |  image = ${jobInfo.image},
         |  seniority = ${jobInfo.seniority},
         |  other = ${jobInfo.other}
         |WHERE id = $jobId
         |""".stripMargin.update.run
      .transact(xa)
      .flatMap(_ => find(jobId))

  override def delete(jobId: JobId): F[Int] =
    sql"""
         |DELETE FROM jobs
         |WHERE id = $jobId
         |""".stripMargin.update.run
      .transact(xa)
}

object JobsRepository {
  def make[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[JobsRepositoryAlg[F]] =
    JobsRepository[F](xa).pure[F]
}
