package com.github.dpratt747
package jobsboard.core.program

import jobsboard.core.repository.UsersRepositoryAlg
import jobsboard.domain.auth.NewPasswordInfo
import jobsboard.domain.job.Email.Email
import jobsboard.domain.job.Password
import jobsboard.domain.job.Password.Password
import jobsboard.domain.user.{Role, User}
import jobsboard.fixtures.UsersGenerators

import cats.data.OptionT
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import org.scalatest.{EitherValues, OptionValues}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration.*

class AuthProgramSpec extends AnyFunSpec with UsersGenerators with Matchers with ScalaCheckPropertyChecks with OptionValues with EitherValues {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  describe("AuthProgram") {
    it("should return None when user is not found") {
      forAll(usersGeneratorsWithPass) { case (user: User, pw: Password) =>
        val userRepo = new UsersRepositoryAlg[IO] {
          override def find(email: Email): IO[Option[User]] = none.pure[IO]

          override def create(user: User): IO[Email] = ???

          override def update(user: User): IO[Option[User]] = ???

          override def delete(email: Email): IO[Boolean] = ???
        }

        val authenticator = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )

        }

        (for {
          program <- AuthProgram.make[IO](userRepo, authenticator)
          login <- program.login(user.email, user.hashedPassword)
        } yield {
          login shouldBe None
        }).unsafeRunSync()


      }
    }
    it("should return none if the user is found but the password is incorrect") {
      forAll(usersGeneratorsWithPass) { case (user: User, pw: Password) =>
        val userRepo = new UsersRepositoryAlg[IO] {
          override def find(email: Email): IO[Option[User]] = user.some.pure[IO]

          override def create(user: User): IO[Email] = ???

          override def update(user: User): IO[Option[User]] = ???

          override def delete(email: Email): IO[Boolean] = ???
        }

        val authenticator = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )

        }

        (for {
          program <- AuthProgram.make[IO](userRepo, authenticator)
          pw <- BCrypt.hashpw[IO](user.hashedPassword.value).map(_.toString)
          login <- program.login(user.email, Password(pw))
        } yield {
          login shouldBe None
        }).unsafeRunSync()
      }
    }
    it("should return a token if the user exists and the password is correct") {
      forAll(usersGeneratorsWithPass) { case (user: User, pw: Password) =>
        val userRepo = new UsersRepositoryAlg[IO] {
          override def find(email: Email): IO[Option[User]] = user.some.pure[IO]

          override def create(user: User): IO[Email] = ???

          override def update(user: User): IO[Option[User]] = ???

          override def delete(email: Email): IO[Boolean] = ???
        }

        val authenticator = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some[IO](user)

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )

        }

        (for {
          program <- AuthProgram.make[IO](userRepo, authenticator)
          login <- program.login(user.email, pw)
        } yield {
          login shouldBe a[Some[Any]]
        }).unsafeRunSync()
      }
    }
    it("should not create a new user during signup if the email is already in use") {
      forAll(usersGeneratorsWithPass) { case (user: User, pw: Password) =>
        val userRepo = new UsersRepositoryAlg[IO] {
          override def find(email: Email): IO[Option[User]] = user.some.pure[IO]

          override def create(user: User): IO[Email] = ???

          override def update(user: User): IO[Option[User]] = ???

          override def delete(email: Email): IO[Boolean] = ???
        }

        val authenticator = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )

        }

        (for {
          program <- AuthProgram.make[IO](userRepo, authenticator)
          signup <- program.signUp(user.email, user.hashedPassword, user.firstName, user.lastName, user.company)
        } yield {
          signup shouldBe a[None.type]
        }).unsafeRunSync()
      }
    }
    it("signup should create a new user") {
      forAll(usersGeneratorsWithPass) { case (user: User, password: Password) =>
        val userRepo = new UsersRepositoryAlg[IO] {
          override def find(email: Email): IO[Option[User]] = none.pure[IO]

          override def create(user: User): IO[Email] = user.email.pure[IO]

          override def update(user: User): IO[Option[User]] = ???

          override def delete(email: Email): IO[Boolean] = ???
        }

        val authenticator = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.none[IO, User]

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )

        }

        (for {
          program <- AuthProgram.make[IO](userRepo, authenticator)
          signup <- program.signUp(user.email, password, user.firstName, user.lastName, user.company)
          pwBool <- BCrypt.checkpwBool[IO](
            password.value,
            PasswordHash[BCrypt](signup.value.hashedPassword.value)
          )
        } yield {
          signup.value.email shouldBe user.email
          signup.value.firstName shouldBe user.firstName
          signup.value.lastName shouldBe user.lastName
          signup.value.company shouldBe user.company
          signup.value.role shouldBe Role.RECRUITER
          pwBool shouldBe true
        }).unsafeRunSync()
      }
    }
    it("should not change the password if the user exists and the old password doesn't matches") {
      forAll(usersGeneratorsWithPass) { case (user: User, password: Password) =>
        val userRepo = new UsersRepositoryAlg[IO] {
          override def find(email: Email): IO[Option[User]] = user.some.pure[IO]

          override def create(user: User): IO[Email] = ???

          override def update(user: User): IO[Option[User]] = ???

          override def delete(email: Email): IO[Boolean] = ???
        }

        val authenticator = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some[IO](user)

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )

        }

        (for {
          program <- AuthProgram.make[IO](userRepo, authenticator)
          changePassword <- program.changePassword(user.email, NewPasswordInfo(user.hashedPassword, Password("new password")))
        } yield {
          changePassword shouldBe Left("Password mismatch, unable to update password")
        }).unsafeRunSync()
      }
    }
    it ("should change the password if the old password matches") {
      forAll(usersGeneratorsWithPass) { case (user: User, password: Password) =>
        val userRepo = new UsersRepositoryAlg[IO] {
          override def find(email: Email): IO[Option[User]] = user.some.pure[IO]

          override def create(user: User): IO[Email] = ???

          override def update(user: User): IO[Option[User]] = user.some.pure[IO]

          override def delete(email: Email): IO[Boolean] = ???
        }

        val authenticator = {
          val key = HMACSHA256.unsafeGenerateKey
          val idStore: IdentityStore[IO, Email, User] = (_: Email) => OptionT.some[IO](user)

          JWTAuthenticator.unbacked.inBearerToken(
            expiryDuration = 1.day,
            maxIdle = None,
            identityStore = idStore,
            signingKey = key
          )
        }

        (for {
          program <- AuthProgram.make[IO](userRepo, authenticator)
          changePassword <- program.changePassword(user.email, NewPasswordInfo(password, Password("new password")))
        } yield {
          changePassword shouldBe a[Right[_, _]]
          changePassword.value shouldBe a[Some[User]]
        }).unsafeRunSync()
      }
    }
  }

}
