package com.github.dpratt747
package jobsboard.domain.job

import jobsboard.domain.job.CompanyName.CompanyName

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.any.DescribedAs
import io.github.iltotore.iron.constraint.string.Match
import io.github.iltotore.iron.{*, given}

object Email {

  type ValidEmail =
    Match["^[a-z0-9][-a-z0-9._]+@([-a-z0-9]+\\.)+[a-z]{2,5}$"] DescribedAs
      "Should be an Email address"

  opaque type Email = String :| ValidEmail

  def unapply(email: String): Option[Email] = email.refineOption[ValidEmail]

  @throws[IllegalArgumentException]
  def apply(value: String): Email = value.refine

  extension (e: Email) def value: String = e

  given Encoder[Email] = Encoder[String].contramap(_.value)

  given Decoder[Email] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given companyNameGet: Get[Email] = Get[String].tmap(apply)

  given companyNamePut: Put[Email] = Put[String].tcontramap(_.value)
}
