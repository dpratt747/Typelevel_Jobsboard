package com.github.dpratt747
package jobsboard.domain.job

import jobsboard.domain.job.Location.*

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


class LocationSpec extends AnyFunSpec with TypeCheckedTripleEquals with Matchers with EitherValues with ScalaCheckDrivenPropertyChecks {

  final case class LocationWrapper(location: Location)

  describe("Location") {
    it("should create a Location") {
      val str = "London"
      val jobId = Location(str)
      jobId.value === str
    }
    it("should not create a Location with an empty string") {
      the[IllegalArgumentException] thrownBy Location("") should have message "Location must not be empty or blank"
    }
    it("should not create a Location with an invalid string") {
      the[IllegalArgumentException] thrownBy Location(" ") should have message "Location must not be empty or blank"
    }
    it("should be able to encode a json") {
      val str = "Remote"
      val toEncode: Location = Location(str)
      val encoded = toEncode.asJson.noSpaces

      encoded === s"$str"
    }
    it("should be able to decode a json") {
      val str = "https://www.google.com"
      val json: String = s"""{"location": "$str"}"""
      val decodeAttempt = decode[LocationWrapper](json)

      decodeAttempt.value.location.value === str
    }
    it("should fail to decode an invalid json") {
      val jsonId: String = """{"location": ""}"""
      val decodeAttempt = decode[LocationWrapper](jsonId)

      decodeAttempt.left.value shouldBe a[io.circe.DecodingFailure]
    }
  }
}
