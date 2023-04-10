package com.github.dpratt747
package jobsboard.domain.job

import jobsboard.domain.job.CompanyName.*

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

class CompanyNameSpec
    extends AnyFunSpec
    with TypeCheckedTripleEquals
    with Matchers
    with EitherValues
    with ScalaCheckDrivenPropertyChecks {

  final case class CompanyNameWrapper(companyName: CompanyName)

  describe("CompanyName") {
    it("should create a CompanyName") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val jobId = CompanyName(str)
        jobId.value === str
      }
    }
    it("should not create a CompanyName with an empty string") {
      the[IllegalArgumentException] thrownBy CompanyName(
        ""
      ) should have message "CompanyName must not be empty or blank"
    }
    it("should not create a CompanyName with a blank string") {
      the[IllegalArgumentException] thrownBy CompanyName(
        "  "
      ) should have message "CompanyName must not be empty or blank"
    }
    it("should be able to encode a CompanyName as json") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val companyName: CompanyName = CompanyName(str)
        val encoded                  = companyName.asJson.noSpaces

        encoded === s"$str"
      }
    }
    it("should be able to decode a company json") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val json: String  = s"""{"companyName": "$str"}"""
        val decodeAttempt = decode[CompanyNameWrapper](json)

        decodeAttempt.value.companyName.value === str
      }
    }
    it("should fail to decode an invalid company json") {
      val jsonId: String = """{"companyName": ""}"""
      val decodeAttempt  = decode[CompanyNameWrapper](jsonId)

      decodeAttempt.left.value shouldBe a[io.circe.DecodingFailure]
    }
  }
}
