package com.github.dpratt747
package jobsboard.core.repository

import jobsboard.domain.pagination.*
import jobsboard.domain.job.*
import jobsboard.fixtures.JobGenerators

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import doobie.implicits.*
import org.scalacheck.Gen
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class JobsSpec
    extends AnyFunSpec
    with Matchers
    with DoobieSpec
    with JobGenerators
    with ScalaCheckPropertyChecks {
  override val initScript: String = "sql/jobs.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  describe("JobsRepository") {
    it("create a job") {
      forAll(jobGen) { job =>
        transactor
          .use { xa =>
            for {
              repo  <- JobsRepository.make(xa)
              _     <- repo.createJob(job.ownerEmail, job.jobInfo)
              count <- sql"""SELECT COUNT(*) FROM jobs""".query[Int].unique.transact(xa)
            } yield {
              count shouldBe 1
            }
          }
          .unsafeRunSync()
      }
    }
    it("deletes a job") {
      forAll(jobGen) { job =>
        transactor
          .use { xa =>
            for {
              repo  <- JobsRepository.make(xa)
              id    <- repo.createJob(job.ownerEmail, job.jobInfo)
              _     <- repo.delete(id)
              count <- sql"""SELECT COUNT(*) FROM jobs""".query[Int].unique.transact(xa)
            } yield {
              count shouldBe 0
            }
          }
          .unsafeRunSync()
      }
    }
    it("gets all jobs") {
      forAll(jobGen) { job =>
        transactor
          .use { xa =>
            for {
              repo <- JobsRepository.make(xa)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              all  <- repo.all()
            } yield {
              all.length shouldBe 3
            }
          }
          .unsafeRunSync()
      }
    }
    it("gets all jobs by filter (company)") {
      forAll(jobGen, companyNameGen) { (job, companyName) =>
        transactor
          .use { xa =>
            for {
              repo <- JobsRepository.make(xa)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo.copy(company = companyName))
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              all  <- repo.all(JobFilter(Some(List(companyName))), Pagination.default)
            } yield {
              all.length shouldBe 1
            }
          }
          .unsafeRunSync()
      }
    }
    it("gets all jobs by filter (location)") {
      forAll(jobGen, locationGen) { (job, location) =>
        transactor
          .use { xa =>
            for {
              repo <- JobsRepository.make(xa)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo.copy(location = location))
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              all  <- repo.all(JobFilter(locations = Some(List(location))), Pagination.default)
            } yield {
              all.length shouldBe 1
            }
          }
          .unsafeRunSync()
      }
    }
    it("gets all jobs by filter (countries)") {
      forAll(jobGen, countryGen) { (job, country) =>
        transactor
          .use { xa =>
            for {
              repo <- JobsRepository.make(xa)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo.copy(country = Some(country)))
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              all  <- repo.all(JobFilter(countries = Some(List(country))), Pagination.default)
            } yield {
              all.length shouldBe 1
            }
          }
          .unsafeRunSync()
      }
    }
    it("gets all jobs by filter (seniority)") {
      forAll(jobGen, seniorityGen) { (job, seniority) =>
        transactor
          .use { xa =>
            for {
              repo <- JobsRepository.make(xa)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo.copy(seniority = Some(seniority)))
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              all  <- repo.all(JobFilter(seniorities = Some(List(seniority))), Pagination.default)
            } yield {
              all.length shouldBe 1
            }
          }
          .unsafeRunSync()
      }
    }
    it("gets all jobs by filter (tags)") {
      forAll(jobGen, nonEmptyTagsList) { (job, tags) =>
        transactor
          .use { xa =>
            for {
              repo <- JobsRepository.make(xa)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo.copy(tags = Some(tags)))
              _    <- repo.createJob(job.ownerEmail, job.jobInfo.copy(tags = None))
              _    <- repo.createJob(job.ownerEmail, job.jobInfo.copy(tags = None))
              all  <- repo.all(JobFilter(tags = Some(tags)), Pagination.default)
            } yield {
              all.length shouldBe 1
            }
          }
          .unsafeRunSync()
      }
    }
    it("gets all jobs by filter (remote)") {
      forAll(jobGen, Gen.prob(0.5)) { (job, remote) =>
        transactor
          .use { xa =>
            for {
              repo <- JobsRepository.make(xa)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo.copy(remote = remote))
              _    <- repo.createJob(job.ownerEmail, job.jobInfo.copy(remote = !remote))
              _    <- repo.createJob(job.ownerEmail, job.jobInfo.copy(remote = !remote))
              all  <- repo.all(JobFilter(remote = Some(remote)), Pagination.default)
            } yield {
              all.length shouldBe 1
            }
          }
          .unsafeRunSync()
      }
    }
    it("gets all jobs with empty filter") {
      forAll(jobGen, companyNameGen) { (job, companyName) =>
        transactor
          .use { xa =>
            for {
              repo <- JobsRepository.make(xa)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo.copy(company = companyName))
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              _    <- repo.createJob(job.ownerEmail, job.jobInfo)
              all  <- repo.all(JobFilter(), Pagination.default)
            } yield {
              all.length shouldBe 3
            }
          }
          .unsafeRunSync()
      }
    }
    it("finds a job by its id") {
      forAll(jobGen) { job =>
        transactor
          .use { xa =>
            for {
              repo   <- JobsRepository.make(xa)
              id     <- repo.createJob(job.ownerEmail, job.jobInfo)
              result <- repo.find(id)
            } yield {
              result.map(_.jobInfo) shouldBe job.jobInfo.some
              result.map(_.ownerEmail) shouldBe job.ownerEmail.some
            }
          }
          .unsafeRunSync()
      }
    }
    it("updates a job") {
      forAll(jobGen, jobGen) { (job, jobForUpdate) =>
        transactor
          .use { xa =>
            for {
              repo   <- JobsRepository.make(xa)
              id     <- repo.createJob(job.ownerEmail, job.jobInfo)
              result <- repo.update(id, jobForUpdate.jobInfo)
            } yield result.map(_.jobInfo) shouldBe jobForUpdate.jobInfo.some
          }
          .unsafeRunSync()
      }
    }
  }

}
