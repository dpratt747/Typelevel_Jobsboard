package com.github.dpratt747
package jobsboard.domain.job

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

object FirstName {
  private type NotEmptyAndBlank = (Not[Empty] & Not[Blank]) DescribedAs "FirstName must not be empty or blank"
  opaque type FirstName = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): FirstName = value.refine

  extension (c: FirstName) def value: String = c

  given Encoder[FirstName] = Encoder[String].contramap(_.value)

  given Decoder[FirstName] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[FirstName] = Get[String].tmap(apply)

  given Put[FirstName] = Put[String].tcontramap(_.value)

}

