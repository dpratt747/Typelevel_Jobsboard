package com.github.dpratt747
package jobsboard.domain.job

import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

import io.circe.{Decoder, Encoder}
import cats.implicits.*
import doobie.*

object Other {
  private type NotEmptyAndBlank = (Not[Empty] & Not[Blank]) DescribedAs "Other must not be empty or blank"
  opaque type Other = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): Other = value.refine

  extension (o: Other) def value: String = o

  given Encoder[Other] = Encoder[String].contramap(_.value)

  given Decoder[Other] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[Other] = Get[String].tmap(apply)

  given Put[Other] = Put[String].tcontramap(_.value)
}

