package com.github.dpratt747
package jobsboard.domain.job

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

object LastName {
  private type NotEmptyAndBlank =
    (Not[Empty] & Not[Blank]) DescribedAs "LastName must not be empty or blank"
  opaque type LastName = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): LastName = value.refine

  extension (c: LastName) def value: String = c

  given Encoder[LastName] = Encoder[String].contramap(_.value)

  given Decoder[LastName] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[LastName] = Get[String].tmap(apply)

  given Put[LastName] = Put[String].tcontramap(_.value)

}
