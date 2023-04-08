package com.github.dpratt747
package jobsboard.http.routes

import jobsboard.core.program.{AuthProgramAlg, JobsProgramAlg}
import jobsboard.domain.auth.{LoginInfo, NewPasswordInfo}
import jobsboard.domain.job.*
import jobsboard.domain.job.CompanyName.CompanyName
import jobsboard.domain.job.Email.Email
import jobsboard.domain.job.FirstName.FirstName
import jobsboard.domain.job.JobId.JobId
import jobsboard.domain.job.LastName.LastName
import jobsboard.domain.job.Password.Password
import jobsboard.domain.pagination.*
import jobsboard.domain.user
import jobsboard.domain.user.*
import jobsboard.fixtures.{JobGenerators, UsersGenerators}
import jobsboard.http.routes.JobRoutes

import cats.data.OptionT
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import com.github.dpratt747.jobsboard.domain.security.Authenticator
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.http4s.{AuthScheme, Credentials, Request, Uri}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.funspec.{AnyFunSpec, AsyncFunSpec}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.{AugmentedJWT, IdentityStore, JWTAuthenticator}
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import java.util.UUID
import scala.concurrent.duration.*

class AuthRoutesSpec extends AnyFunSpec with Matchers with Http4sDsl[IO] with UsersGenerators with ScalaCheckPropertyChecks {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  extension (req: Request[IO]) {
    def withBearerToken(token: AugmentedJWT[HMACSHA256, Email]): Request[IO] = req.putHeaders {
      val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](token.jwt)

      Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
    }
  }

  describe("AuthRoutes") {
    it("[POST] /auth/login should return 401 - Unauthorized if the login fails") {
      forAll(emailGen, passwordGen) { case (email, password) =>

        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = IO.pure(None)

          override def signUp(user: NewUserInfo): IO[Option[User]] = ???

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] = ???

          override def deleteUser(email: Email): IO[Boolean] = ???
        }

        (for {
          routes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          request = Request[IO](method = POST, uri = uri"/auth/login").withEntity(LoginInfo(email, password))
          response <- routes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Unauthorized
        }).unsafeRunSync()
      }
    }
    it("[POST] /auth/login should return 200 - Ok if the login succeeds + JWT token") {
      forAll(emailGen, passwordGen) { case (email, password) =>

        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = mockedAuthenticator.create(email).option

          override def signUp(user: NewUserInfo): IO[Option[User]] = ???

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] = ???

          override def deleteUser(email: Email): IO[Boolean] = ???
        }

        (for {
          routes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          request = Request[IO](method = POST, uri = uri"/auth/login").withEntity(LoginInfo(email, password))
          response <- routes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Ok
          response.headers.get(ci"Authorization").isDefined shouldBe true
        }).unsafeRunSync()
      }
    }
    it("[POST] /auth/user should return 400 - Bad Request if the user already exists") {
      forAll(newUserInfoGen) { case (newUser) =>

        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = ???

          override def signUp(user: NewUserInfo): IO[Option[User]] = IO.pure(None)

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] = ???

          override def deleteUser(email: Email): IO[Boolean] = ???
        }

        (for {
          routes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          request = Request[IO](method = POST, uri = uri"/auth/user").withEntity(newUser)
          response <- routes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe BadRequest
        }).unsafeRunSync()
      }
    }
    it("[POST] /auth/user should return 201 - Created if the user is created") {
      forAll(newUserInfoGen) { case (newUser) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = ???

          override def signUp(user: NewUserInfo): IO[Option[User]] =
            User(user.email, user.password, user.firstName, user.lastName, user.company, Role.RECRUITER).some.pure[IO]

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] = ???

          override def deleteUser(email: Email): IO[Boolean] = ???
        }

        (for {
          routes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          request = Request[IO](method = POST, uri = uri"/auth/user").withEntity(newUser)
          response <- routes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Created
        }).unsafeRunSync()
      }
    }
    it("[POST] /auth/logout should return 401 - unauthorised when logging out without a JWT token") {
      forAll(emailGen, passwordGen) { case (email, password) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = ???

          override def signUp(user: NewUserInfo): IO[Option[User]] = ???

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] = ???

          override def deleteUser(email: Email): IO[Boolean] = ???
        }

        (for {
          authRoutes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          request = Request[IO](method = POST, uri = uri"/auth/logout")
          response <- authRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Unauthorized
        }).unsafeRunSync()
      }
    }
    it("[POST] /auth/logout should return 200 - Ok when logging out with a JWT token") {
      forAll(passwordGen, usersGenerators) { case (password, user) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some[IO](user)

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = ???

          override def signUp(user: NewUserInfo): IO[Option[User]] = ???

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] = ???

          override def deleteUser(email: Email): IO[Boolean] = ???
        }

        (for {
          jwtToken <- mockedAuthenticator.create(user.email)
          authRoutes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          request = Request[IO](method = POST, uri = uri"/auth/logout").withBearerToken(jwtToken)
          response <- authRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Ok
        }).unsafeRunSync()
      }
    }
    it("[PUT] /auth/user/password should return 404 - Not found if the user does not exist") {
      forAll(usersGenerators, passwordGen, passwordGen) { case (user, password, oldPassword) =>

        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some[IO](user)

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = ???

          override def signUp(user: NewUserInfo): IO[Option[User]] = ???

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] =
            none[User].asRight[String].pure[IO]

          override def deleteUser(email: Email): IO[Boolean] = ???
        }
        (for {
          jwtToken <- mockedAuthenticator.create(user.email)
          authRoutes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          request = Request[IO](method = PUT, uri = uri"/auth/user/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(password, oldPassword))
          response <- authRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe NotFound
        }).unsafeRunSync()
      }
    }
    it("[PUT] /auth/user/password should return 403 - Forbidden if the old password is incorrect") {
      forAll(usersGenerators, passwordGen, passwordGen) { case (user, password, oldPassword) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some[IO](user)

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = ???

          override def signUp(user: NewUserInfo): IO[Option[User]] = ???

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] =
            Left("Password mismatch, unable to update password").pure[IO]

          override def deleteUser(email: Email): IO[Boolean] = ???
        }
        (for {
          jwtToken <- mockedAuthenticator.create(user.email)
          authRoutes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          request = Request[IO](method = PUT, uri = uri"/auth/user/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(password, oldPassword))
          response <- authRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Forbidden
        }).unsafeRunSync()
      }
    }
    it("[PUT] /auth/user/password should return 401 - Unauthorized if the jwt token is invalid") {
      forAll(usersGenerators, passwordGen, passwordGen) { case (user, password, oldPassword) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some[IO](user)

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = ???

          override def signUp(user: NewUserInfo): IO[Option[User]] = ???

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] = ???

          override def deleteUser(email: Email): IO[Boolean] = ???
        }

        (for {
          jwtToken <- mockedAuthenticator.create(user.email)
          authRoutes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          request = Request[IO](method = PUT, uri = uri"/auth/user/password")
            .withEntity(NewPasswordInfo(password, oldPassword))
          response <- authRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Unauthorized
        }).unsafeRunSync()
      }
    }
    it("[PUT] /auth/user/password should return 200 - Ok if the password is changed") {
      forAll(emailGen, passwordGen, passwordGen, usersGenerators) { case (email, password, oldPassword, user) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some[IO](user)

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = ???

          override def signUp(user: NewUserInfo): IO[Option[User]] = ???

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] =
            user.some.asRight[String].pure[IO]

          override def deleteUser(email: Email): IO[Boolean] = ???
        }

        (for {
          jwtToken <- mockedAuthenticator.create(email)
          authRoutes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          request = Request[IO](method = PUT, uri = uri"/auth/user/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(password, oldPassword))
          response <- authRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Ok
        }).unsafeRunSync()
      }
    }
    it("[DELETE] /auth/user/<email> should return 401 - Unauthorized if a non admin tries to delete a user") {
      forAll(usersGenerators, emailGen) { case (user, emailToDelete) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some[IO](user.copy(role = Role.RECRUITER))

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = ???

          override def signUp(user: NewUserInfo): IO[Option[User]] = ???

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] = ???

          override def deleteUser(email: Email): IO[Boolean] = ???
        }

        (for {
          jwtToken <- mockedAuthenticator.create(user.email)
          authRoutes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          emailString = emailToDelete.value
          uriString = s"/auth/user/$emailString"
          request = Request[IO](method = DELETE, uri = Uri.unsafeFromString(uriString))
            .withBearerToken(jwtToken)
          response <- authRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Unauthorized
        }).unsafeRunSync()
      }
    }
    it("[DELETE] /auth/user/<email> should return 200 - Ok if an admin tries to delete a user") {
      forAll(usersGenerators, emailGen) { case (user, emailToDelete) =>
        val mockedAuthenticator: JWTAuthenticator[IO, Email, User, HMACSHA256] = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some[IO](user.copy(role = Role.ADMIN))

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        val mockAuthProgram = new AuthProgramAlg[IO] {
          override def authenticator: Authenticator[IO] = mockedAuthenticator

          override def login(email: Email, password: Password): IO[Option[AugmentedJWT[HMACSHA256, Email]]] = ???

          override def signUp(user: NewUserInfo): IO[Option[User]] = ???

          override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] = ???

          override def deleteUser(email: Email): IO[Boolean] = true.pure[IO]
        }

        (for {
          jwtToken <- mockedAuthenticator.create(user.email)
          authRoutes <- AuthRoutes.make[IO](mockAuthProgram, mockedAuthenticator)
          request = Request[IO](method = DELETE, uri = Uri.unsafeFromString(s"/auth/user/${emailToDelete.value}"))
            .withBearerToken(jwtToken)
          response <- authRoutes.routes.orNotFound.run(request)
        } yield {
          response.status shouldBe Ok
        }).unsafeRunSync()
      }
    }
  }

}
