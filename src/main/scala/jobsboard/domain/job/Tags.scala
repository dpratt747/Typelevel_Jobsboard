package com.github.dpratt747
package jobsboard.domain.job

import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

import io.circe.{Decoder, Encoder}
import cats.implicits.*
import doobie.*
import doobie.postgres.implicits.*
import doobie.implicits.*

object Tags {
  private type NotEmptyAndBlank =
    (Not[Empty] & Not[Blank]) DescribedAs "Tag must not be empty or blank"
  opaque type Tags = String :| NotEmptyAndBlank

  @throws[IllegalArgumentException]
  def apply(value: String): Tags = value.refine

  extension (t: Tags) def value: String = t

  given Encoder[Tags] = Encoder[String].contramap(_.value)

  given Decoder[Tags] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[Tags]       = Get[String].tmap(apply)
  given Get[List[Tags]] = Get[List[String]].tmap(list => list.map(apply))
  given Put[List[Tags]] = Put[List[String]].tcontramap(_.map(_.value))
  given Put[Tags]       = Put[String].tcontramap(_.value)
}
