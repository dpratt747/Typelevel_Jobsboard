package com.github.dpratt747
package jobsboard.domain.job

import jobsboard.domain.job.Title.*

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


class TitleSpec extends AnyFunSpec with TypeCheckedTripleEquals with Matchers with EitherValues with ScalaCheckDrivenPropertyChecks {

  final case class TitleWrapper(title: Title)

  describe("Title") {
    it("should create a Title") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val jobId = Title(str)
        jobId.value === str
      }
    }
    it("should not create a Title with an empty string") {
      the[IllegalArgumentException] thrownBy Title("") should have message "Title must not be empty or blank"
    }
    it("should not create a Title with an blank string") {
      the[IllegalArgumentException] thrownBy Title("  ") should have message "Title must not be empty or blank"
    }
    it("should be able to encode a CompanyName as json") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val toEncode: Title = Title(str)
        val encoded = toEncode.asJson.noSpaces

        encoded === s"$str"
      }
    }
    it("should be able to decode a json") {
      forAll(Gen.stringOfN(10, Gen.alphaChar)) { (str: String) =>
        val json: String = s"""{"title": "$str"}"""
        val decodeAttempt = decode[TitleWrapper](json)

        decodeAttempt.value.title.value === str
      }
    }
    it("should fail to decode an invalid json") {
      val jsonId: String = """{"title": ""}"""
      val decodeAttempt = decode[TitleWrapper](jsonId)

      decodeAttempt.left.value shouldBe a[io.circe.DecodingFailure]
    }
  }
}
