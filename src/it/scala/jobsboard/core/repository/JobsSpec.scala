package com.github.dpratt747
package jobsboard.core.repository

import jobsboard.fixtures.JobGenerators

import cats.effect.unsafe.implicits.global
import doobie.implicits.*
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.funspec.AnyFunSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
class JobsSpec extends AnyFunSpec with TypeCheckedTripleEquals with DoobieSpec with JobGenerators with ScalaCheckPropertyChecks {
  override val initScript: String = "sql/init.sql"

  describe("JobsRepository") {
    it("???") {
      forAll(jobGen) { job =>
        rollbackTransactor.use { xa =>
          for {
            repo <- JobsRepository.make(xa)
            _ <- repo.createJob(job.ownerEmail, job.jobInfo)
            count <- sql"""SELECT COUNT(*) FROM jobs""".query[Int].unique.transact(xa)
          } yield {
            count === 1
          }
        }.unsafeRunSync()
      }
    }
  }

}
