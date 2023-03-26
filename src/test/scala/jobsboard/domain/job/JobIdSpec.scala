package com.github.dpratt747
package jobsboard.domain.job

import jobsboard.domain.job.JobId.*

import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import org.scalacheck.Gen
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.util.UUID


class JobIdSpec extends AnyFunSpec with TypeCheckedTripleEquals with Matchers with EitherValues with ScalaCheckDrivenPropertyChecks {

  final case class IdWrapper(id: JobId)
  
  describe("JobId") {
    it("should create a jobId") {
      forAll { (id: UUID) =>
        val jobId = JobId(id.toString)
        jobId.value === id.toString
      }
    }
    it("should not create a jobId") {
      forAll { (id: String) =>
        the[IllegalArgumentException] thrownBy JobId(id) should have message "Should be an UUID"
      }
    }
    it("should be able to encode an id as json") {
      forAll { (id: UUID) =>
        val jobId: JobId = JobId(id.toString)
        val encoded = jobId.asJson.noSpaces

        encoded === s"${id.toString}"
      }
    }
    it("should be able to decode an id json") {
      forAll { (id: UUID) =>
        val jsonId: String = s"""{"id": "${id.toString}"}"""
        val decodeAttempt = decode[IdWrapper](jsonId)

        decodeAttempt.value.id.value === id.toString
      }
    }
    it("should fail to decode an invalid id json") {
      val jsonId: String = """{"id": "this is not a valid id"}"""
      val decodeAttempt = decode[IdWrapper](jsonId)

      decodeAttempt.left.value shouldBe a[io.circe.DecodingFailure]
    }
  }
}
