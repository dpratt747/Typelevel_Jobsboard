package com.github.dpratt747
package jobsboard.domain.job

import cats.implicits.*
import doobie.{Get, Put}
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

object Country {
  private type NotEmptyAndBlank =
    (Not[Empty] & Not[Blank]) DescribedAs "Country must not be empty or blank"
  opaque type Country = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): Country = value.refine

  extension (c: Country) def value: String = c

  given Encoder[Country] = Encoder[String].contramap(_.value)

  given Decoder[Country] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[Country] = Get[String].tmap(apply)

  given Put[Country] = Put[String].tcontramap(_.value)
}
