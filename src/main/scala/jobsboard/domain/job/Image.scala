package com.github.dpratt747
package jobsboard.domain.job

import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

import io.circe.{Decoder, Encoder}
import cats.implicits.*

import doobie.*

object Image {
  private type NotEmptyAndBlank =
    (Not[Empty] & Not[Blank]) DescribedAs "Image must not be empty or blank"
  opaque type Image = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): Image = value.refine

  extension (i: Image) def value: String = i

  given Encoder[Image] = Encoder[String].contramap(_.value)

  given Decoder[Image] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[Image] = Get[String].tmap(apply)

  given Put[Image] = Put[String].tcontramap(_.value)
}
