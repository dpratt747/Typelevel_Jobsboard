package com.github.dpratt747
package jobsboard.domain.job

import cats.implicits.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}
import doobie.*

object Currency {
  private type NotEmptyAndBlank =
    (Not[Empty] & Not[Blank]) DescribedAs "Currency must not be empty or blank"
  opaque type Currency = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): Currency = value.refine

  extension (c: Currency) def value: String = c

  given Encoder[Currency] = Encoder[String].contramap(_.value)

  given Decoder[Currency] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[Currency] = Get[String].tmap(apply)

  given Put[Currency] = Put[String].tcontramap(_.value)
}
