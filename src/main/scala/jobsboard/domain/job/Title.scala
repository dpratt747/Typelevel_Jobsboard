package com.github.dpratt747
package jobsboard.domain.job

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

object Title {
  private type NotEmptyAndBlank =
    (Not[Empty] & Not[Blank]) DescribedAs "Title must not be empty or blank"
  opaque type Title = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): Title = value.refine

  extension (t: Title) def value: String = t

  given Encoder[Title] = Encoder[String].contramap(_.value)

  given Decoder[Title] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[Title] = Get[String].tmap(apply)

  given Put[Title] = Put[String].tcontramap(_.value)
}
