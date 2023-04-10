package com.github.dpratt747
package jobsboard.domain.job

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

object Description {
  private type NotEmptyAndBlank =
    (Not[Empty] & Not[Blank]) DescribedAs "Description must not be empty or blank"
  opaque type Description = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): Description = value.refine

  extension (d: Description) def value: String = d

  given Encoder[Description] = Encoder[String].contramap(_.value)

  given Decoder[Description] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[Description] = Get[String].tmap(apply)

  given Put[Description] = Put[String].tcontramap(_.value)
}
