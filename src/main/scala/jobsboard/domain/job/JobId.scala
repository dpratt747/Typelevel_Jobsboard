package com.github.dpratt747
package jobsboard.domain.job

import cats.implicits.*
import com.github.dpratt747.jobsboard.domain.job.Email.{Email, ValidEmail}
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.{*, given}

import java.util.UUID

object JobId {
  opaque type JobId = String :| ValidUUID

  def unapply(id: String): Option[JobId] = id.refineOption[ValidUUID]
  def unapply(id: UUID): Option[JobId]   = id.toString.refineOption[ValidUUID]

  @throws[IllegalArgumentException]
  def apply(value: String): JobId = value.refine

  @throws[IllegalArgumentException]
  def apply(uuid: UUID): JobId = apply(uuid.toString)

  extension (j: JobId) {
    def value: String = j
    def uuid: UUID    = UUID.fromString(j.value)
  }

  given Encoder[JobId] = Encoder[String].contramap(_.value)

  given Decoder[JobId] = Decoder[String].emap { str =>
    Either.catchNonFatal(apply(str)).leftMap(_.getMessage)
  }

  given Get[JobId] = Get[UUID].tmap(apply)

  given Put[JobId] = Put[UUID].tcontramap(id => UUID.fromString(id.value))

}
