package com.github.dpratt747
package jobsboard.http.routes

import jobsboard.core.program.JobsProgramAlg
import jobsboard.domain.job.*
import jobsboard.domain.job.Email.Email
import jobsboard.domain.job.JobId.JobId
import jobsboard.domain.pagination.*
import jobsboard.fixtures.JobGenerators
import jobsboard.http.routes.JobRoutes

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import io.circe.generic.auto.*
import org.http4s.Request
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.funspec.{AnyFunSpec, AsyncFunSpec}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

class HealthRoutesSpec extends AnyFunSpec with Matchers with Http4sDsl[IO] with JobGenerators with ScalaCheckPropertyChecks {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  describe("HealthRoutes") {
    it("[GET] /health should return ok") {
      (for {
        routes <- HealthRoutes.make[IO]()
        request = Request[IO](method = GET, uri = uri"/health")
        response <- routes.routes.orNotFound.run(request)
      } yield {
        response.status shouldBe Ok
      }).unsafeRunSync()
    }
  }

}
