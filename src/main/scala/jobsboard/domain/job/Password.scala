package com.github.dpratt747
package jobsboard.domain.job

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

object Password {
  private type NotEmptyAndBlank =
    (Not[Empty] & Not[Blank]) DescribedAs "LastName must not be empty or blank"
  opaque type Password = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): Password = value.refine

  extension (c: Password) def value: String = c

  given Encoder[Password] = Encoder[String].contramap(_.value)

  given Decoder[Password] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[Password] = Get[String].tmap(apply)

  given Put[Password] = Put[String].tcontramap(_.value)

}
