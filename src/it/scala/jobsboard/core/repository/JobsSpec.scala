package com.github.dpratt747
package jobsboard.core.repository

import jobsboard.fixtures.JobGenerators

import cats.effect.unsafe.implicits.global
import cats.implicits.*
import doobie.implicits.*
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class JobsSpec extends AnyFunSpec with Matchers with DoobieSpec with JobGenerators with ScalaCheckPropertyChecks {
  override val initScript: String = "sql/jobs.sql"

  describe("JobsRepository") {
    it("create a job") {
      forAll(jobGen) { job =>
        transactor.use { xa =>
          for {
            repo <- JobsRepository.make(xa)
            _ <- repo.createJob(job.ownerEmail, job.jobInfo)
            count <- sql"""SELECT COUNT(*) FROM jobs""".query[Int].unique.transact(xa)
          } yield {
            count shouldBe 1
          }
        }.unsafeRunSync()
      }
    }
    it("deletes a job") {
      forAll(jobGen) { job =>
        transactor.use { xa =>
          for {
            repo <- JobsRepository.make(xa)
            id <- repo.createJob(job.ownerEmail, job.jobInfo)
            _ <- repo.delete(id)
            count <- sql"""SELECT COUNT(*) FROM jobs""".query[Int].unique.transact(xa)
          } yield {
            count shouldBe 0
          }
        }.unsafeRunSync()
      }
    }
    it("gets all jobs") {
      forAll(jobGen) { job =>
        transactor.use { xa =>
          for {
            repo <- JobsRepository.make(xa)
            _ <- repo.createJob(job.ownerEmail, job.jobInfo)
            _ <- repo.createJob(job.ownerEmail, job.jobInfo)
            _ <- repo.createJob(job.ownerEmail, job.jobInfo)
            all <- repo.all()
          } yield {
            all.length shouldBe 3
          }
        }.unsafeRunSync()
      }
    }
    it("finds a job by its id") {
      forAll(jobGen) { job =>
        transactor.use { xa =>
          for {
            repo <- JobsRepository.make(xa)
            id <- repo.createJob(job.ownerEmail, job.jobInfo)
            result <- repo.find(id)
          } yield {
            result.map(_.jobInfo) shouldBe job.jobInfo.some
            result.map(_.ownerEmail) shouldBe job.ownerEmail.some
          }
        }.unsafeRunSync()
      }
    }
    it("updates a job") {
      forAll(jobGen, jobGen) { (job, jobForUpdate) =>
        transactor.use { xa =>
          for {
            repo <- JobsRepository.make(xa)
            id <- repo.createJob(job.ownerEmail, job.jobInfo)
            result <- repo.update(id, jobForUpdate.jobInfo)
          } yield
            result.map(_.jobInfo) shouldBe jobForUpdate.jobInfo.some
        }.unsafeRunSync()
      }
    }
  }

}
