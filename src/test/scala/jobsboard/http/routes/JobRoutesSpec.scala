package com.github.dpratt747
package jobsboard.http.routes

import jobsboard.core.program.JobsProgramAlg
import jobsboard.domain.job.*
import jobsboard.domain.job.Email.Email
import jobsboard.domain.job.JobId.JobId
import jobsboard.domain.pagination.*
import jobsboard.domain.user.{Role, User}
import jobsboard.fixtures.{JobGenerators, UsersGenerators}
import jobsboard.http.routes.JobRoutes

import cats.data.OptionT
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
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256

import java.util.UUID
import scala.concurrent.duration.*

class JobRoutesSpec
    extends AnyFunSpec
    with Matchers
    with Http4sDsl[IO]
    with UsersGenerators
    with ScalaCheckPropertyChecks
    with RequestUtil {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  describe("JobRoutes") {
    it("[POST] /jobs should return a list of jobs") {
      forAll(jobGen) { job =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key                                     = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val jobsService = new JobsProgramAlg[IO] {
          def insertJob(ownerEmail: Email, jobsInfo: JobInfo): IO[JobId] = ???

          def getAll(): IO[List[Job]] = ???

          def getAll(filter: JobFilter, pagination: Pagination): IO[List[Job]] = List(job).pure[IO]

          def findByJobId(jobId: JobId): IO[Option[Job]] = ???

          def updateJob(jobId: JobId, jobInfo: JobInfo): IO[Option[Job]] = ???

          def delete(jobId: JobId): IO[Int] = ???
        }

        (for {
          jobRoutes <- JobRoutes.make[IO](jobsService, mockedAuthenticator)
          request = Request[IO](method = POST, uri = uri"/jobs")
            .withEntity(JobFilter())
          response <- jobRoutes.routes.orNotFound.run(request)
          body     <- response.as[List[Job]]
        } yield {
          response.status shouldBe Ok
          body shouldBe List(job)
        }).unsafeRunSync()
      }
    }
    it("[GET] /jobs/<uuid> should return a jobs if it exists") {
      forAll(jobIdGen, jobGen) { (jobId, job) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key                                     = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val jobsService = new JobsProgramAlg[IO] {
          def insertJob(ownerEmail: Email, jobsInfo: JobInfo): IO[JobId] = ???

          def getAll(): IO[List[Job]] = ???

          def getAll(filter: JobFilter, pagination: Pagination): IO[List[Job]] = ???

          def findByJobId(jobId: JobId): IO[Option[Job]] = job.some.pure[IO]

          def updateJob(jobId: JobId, jobInfo: JobInfo): IO[Option[Job]] = ???

          def delete(jobId: JobId): IO[Int] = ???
        }

        (for {
          jobRoutes <- JobRoutes.make[IO](jobsService, mockedAuthenticator)
          request = Request[IO](method = GET, uri = uri"/jobs/".addPath(jobId.value))
          response <- jobRoutes.routes.orNotFound.run(request)
          body     <- response.as[Job]
        } yield {
          response.status shouldBe Ok
          body shouldBe job
        }).unsafeRunSync()
      }
    }
    it("[GET] /jobs/<uuid> should return a 404 if the job does not exist") {
      forAll(jobIdGen) { jobId =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key                                     = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val jobsService = new JobsProgramAlg[IO] {
          def insertJob(ownerEmail: Email, jobsInfo: JobInfo): IO[JobId] = ???

          def getAll(): IO[List[Job]] = ???

          def getAll(filter: JobFilter, pagination: Pagination): IO[List[Job]] = ???

          def findByJobId(jobId: JobId): IO[Option[Job]] = None.pure[IO]

          def updateJob(jobId: JobId, jobInfo: JobInfo): IO[Option[Job]] = ???

          def delete(jobId: JobId): IO[Int] = ???
        }

        (for {
          jobRoutes <- JobRoutes.make[IO](jobsService, mockedAuthenticator)
          request = Request[IO](method = GET, uri = uri"/jobs/".addPath(jobId.value))
          response <- jobRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe NotFound
        }).unsafeRunSync()
      }
    }
    it("[POST] /jobs/create should create a job") {
      forAll(jobInfoGen, jobIdGen, usersGenerators) { (jobInfo, jobId, user) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key                                     = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some(user)

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val jobsService = new JobsProgramAlg[IO] {
          def insertJob(ownerEmail: Email, jobsInfo: JobInfo): IO[JobId] = jobId.pure[IO]

          def getAll(): IO[List[Job]] = ???

          def getAll(filter: JobFilter, pagination: Pagination): IO[List[Job]] = ???

          def findByJobId(jobId: JobId): IO[Option[Job]] = ???

          def updateJob(jobId: JobId, jobInfo: JobInfo): IO[Option[Job]] = ???

          def delete(jobId: JobId): IO[Int] = ???
        }

        (for {
          token     <- mockedAuthenticator.create(user.email)
          jobRoutes <- JobRoutes.make[IO](jobsService, mockedAuthenticator)
          request = Request[IO](method = POST, uri = uri"/jobs/create")
            .withBearerToken(token)
            .withEntity(jobInfo)
          response <- jobRoutes.routes.orNotFound.run(request)
          body     <- response.as[JobId]
        } yield {
          response.status shouldBe Created
          body shouldBe jobId
        }).unsafeRunSync()
      }
    }
    it("[PUT] /jobs/update/<uuid> should update a job") {
      forAll(jobIdGen, jobInfoGen, jobGen, usersGenerators) { (jobId, jobInfo, job, user) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] =
            (_: Email) => OptionT.some(user.copy(role = Role.ADMIN))

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val jobsService = new JobsProgramAlg[IO] {
          def insertJob(ownerEmail: Email, jobsInfo: JobInfo): IO[JobId] = ???

          def getAll(): IO[List[Job]] = ???

          def getAll(filter: JobFilter, pagination: Pagination): IO[List[Job]] = ???

          def findByJobId(jobId: JobId): IO[Option[Job]] = job.some.pure[IO]

          def updateJob(jobId: JobId, jobInfo: JobInfo): IO[Option[Job]] = job.some.pure[IO]

          def delete(jobId: JobId): IO[Int] = ???
        }

        (for {
          token     <- mockedAuthenticator.create(user.email)
          jobRoutes <- JobRoutes.make[IO](jobsService, mockedAuthenticator)
          request = Request[IO](method = PUT, uri = uri"/jobs/update".addPath(jobId.value))
            .withBearerToken(token)
            .withEntity(jobInfo)
          response <- jobRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Ok
        }).unsafeRunSync()
      }
    }
    it("[PUT] /jobs/update/<uuid> should return a 404 if the job does not exist") {
      forAll(jobIdGen, jobInfoGen, usersGenerators) { (jobId, jobInfo, user) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] =
            (_: Email) => OptionT.some(user.copy(role = Role.ADMIN))

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val jobsService = new JobsProgramAlg[IO] {
          def insertJob(ownerEmail: Email, jobsInfo: JobInfo): IO[JobId] = ???

          def getAll(): IO[List[Job]] = ???

          def getAll(filter: JobFilter, pagination: Pagination): IO[List[Job]] = ???

          def findByJobId(jobId: JobId): IO[Option[Job]] = None.pure[IO]

          def updateJob(jobId: JobId, jobInfo: JobInfo): IO[Option[Job]] = None.pure[IO]

          def delete(jobId: JobId): IO[Int] = ???
        }

        (for {
          token     <- mockedAuthenticator.create(user.email)
          jobRoutes <- JobRoutes.make[IO](jobsService, mockedAuthenticator)
          request = Request[IO](method = PUT, uri = uri"/jobs/update".addPath(jobId.value))
            .withBearerToken(token)
            .withEntity(jobInfo)
          response <- jobRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe NotFound
        }).unsafeRunSync()
      }
    }
    it("[DELETE] /jobs/delete/<uuid> should delete a job if it exists and the user is the owner") {
      forAll(jobIdGen, jobGen, usersGenerators) { (jobId, job, user) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key                                     = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some(user)

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val jobsService = new JobsProgramAlg[IO] {
          def insertJob(ownerEmail: Email, jobsInfo: JobInfo): IO[JobId] = ???

          def getAll(): IO[List[Job]] = ???

          def getAll(filter: JobFilter, pagination: Pagination): IO[List[Job]] = ???

          def findByJobId(jobId: JobId): IO[Option[Job]] =
            job.copy(ownerEmail = user.email).some.pure[IO]

          def updateJob(jobId: JobId, jobInfo: JobInfo): IO[Option[Job]] = ???

          def delete(jobId: JobId): IO[Int] = 1.pure[IO]
        }

        (for {
          token     <- mockedAuthenticator.create(user.email)
          jobRoutes <- JobRoutes.make[IO](jobsService, mockedAuthenticator)
          request = Request[IO](method = DELETE, uri = uri"/jobs".addPath(jobId.value))
            .withBearerToken(token)
          response <- jobRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Ok
        }).unsafeRunSync()
      }
    }
    it(
      "[DELETE] /jobs/delete/<uuid> should return 403 - Forbidden if it exists but the user is not the owner and not admin"
    ) {
      forAll(jobIdGen, jobGen, usersGenerators) { (jobId, job, user) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] =
            (_: Email) => OptionT.some(user.copy(role = Role.RECRUITER))

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val jobsService = new JobsProgramAlg[IO] {
          def insertJob(ownerEmail: Email, jobsInfo: JobInfo): IO[JobId] = ???

          def getAll(): IO[List[Job]] = ???

          def getAll(filter: JobFilter, pagination: Pagination): IO[List[Job]] = ???

          def findByJobId(jobId: JobId): IO[Option[Job]] =
            job.copy(ownerEmail = user.email).some.pure[IO]

          def updateJob(jobId: JobId, jobInfo: JobInfo): IO[Option[Job]] = ???

          def delete(jobId: JobId): IO[Int] = 1.pure[IO]
        }

        (for {
          token     <- mockedAuthenticator.create(user.email)
          jobRoutes <- JobRoutes.make[IO](jobsService, mockedAuthenticator)
          request = Request[IO](method = DELETE, uri = uri"/jobs".addPath(jobId.value))
            .withBearerToken(token)
          response <- jobRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Ok
        }).unsafeRunSync()
      }
    }
    it("[DELETE] /jobs/delete/<uuid> should return a 404 if the job does not exist") {
      forAll(jobIdGen, usersGenerators) { (jobId, user) =>

        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key                                     = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some(user)

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val jobsService = new JobsProgramAlg[IO] {
          def insertJob(ownerEmail: Email, jobsInfo: JobInfo): IO[JobId] = ???

          def getAll(): IO[List[Job]] = ???

          def getAll(filter: JobFilter, pagination: Pagination): IO[List[Job]] = ???

          def findByJobId(jobId: JobId): IO[Option[Job]] = None.pure[IO]

          def updateJob(jobId: JobId, jobInfo: JobInfo): IO[Option[Job]] = ???

          def delete(jobId: JobId): IO[Int] = ???
        }

        (for {
          token     <- mockedAuthenticator.create(user.email)
          jobRoutes <- JobRoutes.make[IO](jobsService, mockedAuthenticator)
          request = Request[IO](method = DELETE, uri = uri"/jobs".addPath(jobId.value))
            .withBearerToken(token)
          response <- jobRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe NotFound
        }).unsafeRunSync()
      }
    }
  }

}
