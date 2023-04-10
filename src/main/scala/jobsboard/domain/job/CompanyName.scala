package com.github.dpratt747
package jobsboard.domain.job

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

object CompanyName {
  private type NotEmptyAndBlank =
    (Not[Empty] & Not[Blank]) DescribedAs "CompanyName must not be empty or blank"
  opaque type CompanyName = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): CompanyName = value.refine

  extension (c: CompanyName) def value: String = c

  given Encoder[CompanyName] = Encoder[String].contramap(_.value)

  given Decoder[CompanyName] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[CompanyName] = Get[String].tmap(apply)

  given Put[CompanyName] = Put[String].tcontramap(_.value)

}
