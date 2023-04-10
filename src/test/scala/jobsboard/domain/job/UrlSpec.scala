package com.github.dpratt747
package jobsboard.domain.job

import jobsboard.domain.job.URL.*

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

class UrlSpec
    extends AnyFunSpec
    with TypeCheckedTripleEquals
    with Matchers
    with EitherValues
    with ScalaCheckDrivenPropertyChecks {

  final case class UrlWrapper(url: URL)

  describe("URL") {
    it("should create a URL") {
      val str   = "https://www.google.com"
      val jobId = URL(str)
      jobId.value === str
    }
    it("should not create a URL with an empty string") {
      the[IllegalArgumentException] thrownBy URL("") should have message "Should be an URL"
    }
    it("should be able to encode a json") {
      val str           = "https://www.google.com"
      val toEncode: URL = URL(str)
      val encoded       = toEncode.asJson.noSpaces

      encoded === s"$str"
    }
    it("should be able to decode a json") {
      val str           = "https://www.google.com"
      val json: String  = s"""{"url": "$str"}"""
      val decodeAttempt = decode[UrlWrapper](json)

      decodeAttempt.value.url.value === str
    }
    it("should fail to decode an invalid json") {
      val jsonId: String = """{"url": ""}"""
      val decodeAttempt  = decode[UrlWrapper](jsonId)

      decodeAttempt.left.value shouldBe a[io.circe.DecodingFailure]
    }
  }
}
