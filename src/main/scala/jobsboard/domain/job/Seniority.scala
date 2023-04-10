package com.github.dpratt747
package jobsboard.domain.job

import cats.implicits.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}
import doobie.*

object Seniority {
  private type NotEmptyAndBlank =
    (Not[Empty] & Not[Blank]) DescribedAs "Seniority must not be empty or blank"
  opaque type Seniority = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): Seniority = value.refine

  extension (s: Seniority) def value: String = s

  given Encoder[Seniority] = Encoder[String].contramap(_.value)

  given Decoder[Seniority] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[Seniority] = Get[String].tmap(apply)

  given Put[Seniority] = Put[String].tcontramap(_.value)
}
