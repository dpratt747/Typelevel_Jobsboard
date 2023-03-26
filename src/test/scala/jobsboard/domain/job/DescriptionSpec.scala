package com.github.dpratt747
package jobsboard.domain.job

import jobsboard.domain.job.Description.*

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


class DescriptionSpec extends AnyFunSpec with TypeCheckedTripleEquals with Matchers with EitherValues with ScalaCheckDrivenPropertyChecks {

  final case class DescriptionWrapper(description: Description)

  describe("Description") {
    it("should create a Description") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val jobId = Description(str)
        jobId.value === str
      }
    }
    it("should not create a Description with an empty string") {
      the[IllegalArgumentException] thrownBy Description("") should have message "Description must not be empty or blank"
    }
    it("should not create a Description with an blank string") {
      the[IllegalArgumentException] thrownBy Description("  ") should have message "Description must not be empty or blank"
    }
    it("should be able to encode a json") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val toEncode: Description = Description(str)
        val encoded = toEncode.asJson.noSpaces

        encoded === s"$str"
      }
    }
    it("should be able to decode a json") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val json: String = s"""{"description": "$str"}"""
        val decodeAttempt = decode[DescriptionWrapper](json)

        decodeAttempt.value.description.value === str
      }
    }
    it("should fail to decode an invalid json") {
      val jsonId: String = """{"description": ""}"""
      val decodeAttempt = decode[DescriptionWrapper](jsonId)

      decodeAttempt.left.value shouldBe a[io.circe.DecodingFailure]
    }
  }
}
