package com.github.dpratt747
package jobsboard.domain.job

import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

import io.circe.{Decoder, Encoder}
import cats.implicits.*
import doobie.*

object SalaryHigh {
  opaque type SalaryHigh = Int :| GreaterEqual[0]

  @throws[IllegalArgumentException]
  def apply(value: Int): SalaryHigh = value.refine

  extension (s: SalaryHigh) def value: Int = s

  given Encoder[SalaryHigh] = Encoder[Int].contramap(_.value)

  given Decoder[SalaryHigh] = Decoder[Int].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[SalaryHigh] = Get[Int].tmap(apply)

  given Put[SalaryHigh] = Put[Int].tcontramap(_.value)
}

