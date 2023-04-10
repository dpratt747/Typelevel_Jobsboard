package com.github.dpratt747
package jobsboard.domain.job

import jobsboard.domain.job.Email.*

import io.circe.parser.*
import io.circe.syntax.*
import io.circe.generic.auto.*

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class EmailSpec extends AnyFunSpec with TypeCheckedTripleEquals with Matchers with EitherValues {

  final case class EmailWrapper(email: Email)

  describe("Email") {
    it("should create an email") {
      val email = Email("validemail@email.com")
      email.value === "somevalid@email.com"
    }
    it("should not create an email") {
      the[IllegalArgumentException] thrownBy Email(
        ""
      ) should have message "Should be an Email address"
    }
    it("should be able to encode an email as json") {
      val email: Email = Email("somevalid@mail.com")
      val encoded      = email.asJson.noSpaces

      encoded === "\"somevalid@email.com\""
    }
    it("should be able to decode an email json") {
      val jsonEmail: String = """{"email": "somevalid@mail.com"}"""
      val decodeAttempt     = decode[EmailWrapper](jsonEmail)

      decodeAttempt.value.email.value === "somevalid@email.com"
    }
    it("should fail to decode an invalid email json") {
      val jsonEmail: String = """{"email": "this is not a valid email address"}"""
      val decodeAttempt     = decode[EmailWrapper](jsonEmail)

      decodeAttempt.left.value shouldBe a[io.circe.DecodingFailure]
    }
  }

}
