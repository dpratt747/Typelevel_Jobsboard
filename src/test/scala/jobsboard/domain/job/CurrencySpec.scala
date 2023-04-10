package com.github.dpratt747
package jobsboard.domain.job

import jobsboard.domain.job.Currency.*

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

class CurrencySpec
    extends AnyFunSpec
    with TypeCheckedTripleEquals
    with Matchers
    with EitherValues
    with ScalaCheckDrivenPropertyChecks {

  final case class CurrencyWrapper(currency: Currency)

  describe("Currency") {
    it("should create a Currency") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val jobId = Currency(str)
        jobId.value === str
      }
    }
    it("should not create a Currency with an empty string") {
      the[IllegalArgumentException] thrownBy Currency(
        ""
      ) should have message "Currency must not be empty or blank"
    }
    it("should not create a Currency with a blank string") {
      the[IllegalArgumentException] thrownBy Currency(
        "  "
      ) should have message "Currency must not be empty or blank"
    }
    it("should be able to encode a Currency as json") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val currency: Currency = Currency(str)
        val encoded            = currency.asJson.noSpaces

        encoded === s"$str"
      }
    }
    it("should be able to decode a currency json") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val json: String  = s"""{"currency": "$str"}"""
        val decodeAttempt = decode[CurrencyWrapper](json)

        decodeAttempt.value.currency.value === str
      }
    }
    it("should fail to decode an invalid currency json") {
      val jsonId: String = """{"currency": ""}"""
      val decodeAttempt  = decode[CurrencyWrapper](jsonId)

      decodeAttempt.left.value shouldBe a[io.circe.DecodingFailure]
    }
  }
}
