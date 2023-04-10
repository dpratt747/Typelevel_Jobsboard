package com.github.dpratt747
package jobsboard.domain.job

import jobsboard.domain.job.Description.Description

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

object URL {
  opaque type URL = String :| ValidURL

  def apply(value: String): URL = value.refine

  extension (u: URL) def value: String = u

  given Encoder[URL] = Encoder[String].contramap(_.value)

  given Decoder[URL] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given urlGet: Get[URL] = Get[String].tmap(apply)

  given urlPut: Put[URL] = Put[String].tcontramap(_.value)
}
