package com.github.dpratt747
package jobsboard.domain.job

import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

import io.circe.{Decoder, Encoder}
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*

object Location {
  private type NotEmptyAndBlank =
    (Not[Empty] & Not[Blank]) DescribedAs "Location must not be empty or blank"
  opaque type Location = String :| NotEmptyAndBlank

  def apply(value: String): Location = value.refine

  extension (l: Location) def value: String = l

  given Encoder[Location] = Encoder[String].contramap(_.value)

  given Decoder[Location] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[Location] = Get[String].tmap(apply)

  given Put[Location] = Put[String].tcontramap(_.value)
}
