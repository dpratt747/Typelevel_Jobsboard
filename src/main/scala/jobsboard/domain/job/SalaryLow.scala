package com.github.dpratt747
package jobsboard.domain.job

import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

import io.circe.{Decoder, Encoder}
import cats.implicits.*
import doobie.*

object SalaryLow {
  opaque type SalaryLow = Int :| GreaterEqual[0]

  @throws[IllegalArgumentException]
  def apply(value: Int): SalaryLow = value.refine

  extension (s: SalaryLow) def value: Int = s

  given Encoder[SalaryLow] = Encoder[Int].contramap(_.value)

  given Decoder[SalaryLow] = Decoder[Int].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[SalaryLow] = Get[Int].tmap(apply)

  given Put[SalaryLow] = Put[Int].tcontramap(_.value)
}
