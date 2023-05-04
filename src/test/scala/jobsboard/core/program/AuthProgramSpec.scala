package com.github.dpratt747
package jobsboard.core.program

import jobsboard.config.*
import jobsboard.core.repository.UsersRepositoryAlg
import jobsboard.domain.auth.NewPasswordInfo
import jobsboard.domain.job.Email.Email
import jobsboard.domain.job.Password
import jobsboard.domain.job.Password.Password
import jobsboard.domain.user.{NewUserInfo, Role, User}
import jobsboard.fixtures.UsersGenerators

import cats.data.OptionT
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import com.comcast.ip4s.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration.*

class AuthProgramSpec
    extends AnyFunSpec
    with UsersGenerators
    with Matchers
    with ScalaCheckPropertyChecks
    with OptionValues
    with EitherValues {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val mockConfig: ApplicationConfig = ApplicationConfig(
    SecurityConfig("secret", 1.day),
    EmberConfig(Host.fromString("0.0.0.0").get, Port.fromInt(4200).get),
    PostgresConfig(
      1,
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5432/board",
      "docker",
      "docker"
    ),
    TokenConfig(1000),
    EmailServiceConfig(
      "smtp.gmail.com",
      587,
      " ",
      " ",
      " "
    )
  )

  describe("AuthProgram") {
    it("should return None when user is not found") {
      forAll(usersGeneratorsWithPass) { case (user: User, pw: Password) =>
        val mockUserProgram: UsersProgramAlg[IO] = new UsersProgramAlg[IO] {
          override def findUser(email: Email): IO[Option[User]] = none.pure[IO]

          override def createUser(user: User): IO[Email] = ???

          override def updateUser(user: User): IO[Option[User]] = ???

          override def deleteUser(email: Email): IO[Boolean] = false.pure[IO]
        }

        val mockTokenProgram: TokensProgramAlg[IO] = new TokensProgramAlg[IO] {
          def getToken(email: Email): IO[Option[String]] = ???

          def checkToken(email: Email, token: String): IO[Boolean] = ???
        }

        val mockEmailProgram: EmailsProgramAlg[IO] = new EmailsProgramAlg[IO] {
          def sendEmail(to: Email, subject: String, content: String): IO[Unit] = ???

          def sendPasswordRecoveryEmail(to: Email, token: String): IO[Unit] = ???
        }

        (for {
          program <- AuthProgram.make[IO](
            mockUserProgram,
            mockTokenProgram,
            mockEmailProgram,
            mockConfig
          )
          login <- program.login(user.email, user.hashedPassword)
        } yield {
          login shouldBe None
        }).unsafeRunSync()

      }
    }
    it("should return none if the user is found but the password is incorrect") {
      forAll(usersGeneratorsWithPass) { case (user: User, pw: Password) =>
        val mockUserProgram: UsersProgramAlg[IO] = new UsersProgramAlg[IO] {
          override def findUser(email: Email): IO[Option[User]] = user.some.pure[IO]

          override def createUser(user: User): IO[Email] = ???

          override def updateUser(user: User): IO[Option[User]] = ???

          override def deleteUser(email: Email): IO[Boolean] = false.pure[IO]
        }

        val mockTokenProgram: TokensProgramAlg[IO] = new TokensProgramAlg[IO] {
          def getToken(email: Email): IO[Option[String]] = ???

          def checkToken(email: Email, token: String): IO[Boolean] = ???
        }

        val mockEmailProgram: EmailsProgramAlg[IO] = new EmailsProgramAlg[IO] {
          def sendEmail(to: Email, subject: String, content: String): IO[Unit] = ???

          def sendPasswordRecoveryEmail(to: Email, token: String): IO[Unit] = ???
        }

        (for {
          program <- AuthProgram.make[IO](
            mockUserProgram,
            mockTokenProgram,
            mockEmailProgram,
            mockConfig
          )
          pw    <- BCrypt.hashpw[IO](user.hashedPassword.value).map(_.toString)
          login <- program.login(user.email, Password(pw))
        } yield {
          login shouldBe None
        }).unsafeRunSync()
      }
    }
    it("should return a token if the user exists and the password is correct") {
      forAll(usersGeneratorsWithPass) { case (user: User, pw: Password) =>
        val mockUserProgram: UsersProgramAlg[IO] = new UsersProgramAlg[IO] {
          override def findUser(email: Email): IO[Option[User]] = user.some.pure[IO]

          override def createUser(user: User): IO[Email] = ???

          override def updateUser(user: User): IO[Option[User]] = ???

          override def deleteUser(email: Email): IO[Boolean] = false.pure[IO]
        }

        val mockTokenProgram: TokensProgramAlg[IO] = new TokensProgramAlg[IO] {
          def getToken(email: Email): IO[Option[String]] = ???

          def checkToken(email: Email, token: String): IO[Boolean] = ???
        }

        val mockEmailProgram: EmailsProgramAlg[IO] = new EmailsProgramAlg[IO] {
          def sendEmail(to: Email, subject: String, content: String): IO[Unit] = ???

          def sendPasswordRecoveryEmail(to: Email, token: String): IO[Unit] = ???
        }

        (for {
          program <- AuthProgram.make[IO](
            mockUserProgram,
            mockTokenProgram,
            mockEmailProgram,
            mockConfig
          )
          login <- program.login(user.email, pw)
        } yield {
          login shouldBe a[Some[Any]]
        }).unsafeRunSync()
      }
    }
    it("should not create a new user during signup if the email is already in use") {
      forAll(usersGeneratorsWithPass) { case (user: User, pw: Password) =>
        val mockUserProgram: UsersProgramAlg[IO] = new UsersProgramAlg[IO] {
          override def findUser(email: Email): IO[Option[User]] = user.some.pure[IO]

          override def createUser(user: User): IO[Email] = ???

          override def updateUser(user: User): IO[Option[User]] = ???

          override def deleteUser(email: Email): IO[Boolean] = false.pure[IO]
        }

        val mockTokenProgram: TokensProgramAlg[IO] = new TokensProgramAlg[IO] {
          def getToken(email: Email): IO[Option[String]] = ???

          def checkToken(email: Email, token: String): IO[Boolean] = ???
        }

        val mockEmailProgram: EmailsProgramAlg[IO] = new EmailsProgramAlg[IO] {
          def sendEmail(to: Email, subject: String, content: String): IO[Unit] = ???

          def sendPasswordRecoveryEmail(to: Email, token: String): IO[Unit] = ???
        }

        (for {
          program <- AuthProgram.make[IO](
            mockUserProgram,
            mockTokenProgram,
            mockEmailProgram,
            mockConfig
          )
          signup <- program.signUp(
            NewUserInfo(
              user.email,
              user.hashedPassword,
              user.firstName,
              user.lastName,
              user.company
            )
          )
        } yield {
          signup shouldBe a[None.type]
        }).unsafeRunSync()
      }
    }
    it("signup should create a new user") {
      forAll(usersGeneratorsWithPass) { case (user: User, password: Password) =>
        val mockUserProgram: UsersProgramAlg[IO] = new UsersProgramAlg[IO] {
          override def findUser(email: Email): IO[Option[User]] = none.pure[IO]

          override def createUser(user: User): IO[Email] = user.email.pure[IO]

          override def updateUser(user: User): IO[Option[User]] = ???

          override def deleteUser(email: Email): IO[Boolean] = false.pure[IO]
        }

        val mockTokenProgram: TokensProgramAlg[IO] = new TokensProgramAlg[IO] {
          def getToken(email: Email): IO[Option[String]] = ???

          def checkToken(email: Email, token: String): IO[Boolean] = ???
        }

        val mockEmailProgram: EmailsProgramAlg[IO] = new EmailsProgramAlg[IO] {
          def sendEmail(to: Email, subject: String, content: String): IO[Unit] = ???

          def sendPasswordRecoveryEmail(to: Email, token: String): IO[Unit] = ???
        }

        (for {
          program <- AuthProgram.make[IO](
            mockUserProgram,
            mockTokenProgram,
            mockEmailProgram,
            mockConfig
          )
          signup <- program.signUp(
            NewUserInfo(user.email, password, user.firstName, user.lastName, user.company)
          )
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
        val mockUserProgram: UsersProgramAlg[IO] = new UsersProgramAlg[IO] {
          override def findUser(email: Email): IO[Option[User]] = user.some.pure[IO]

          override def createUser(user: User): IO[Email] = ???

          override def updateUser(user: User): IO[Option[User]] = ???

          override def deleteUser(email: Email): IO[Boolean] = false.pure[IO]
        }

        val mockTokenProgram: TokensProgramAlg[IO] = new TokensProgramAlg[IO] {
          def getToken(email: Email): IO[Option[String]] = ???

          def checkToken(email: Email, token: String): IO[Boolean] = ???
        }

        val mockEmailProgram: EmailsProgramAlg[IO] = new EmailsProgramAlg[IO] {
          def sendEmail(to: Email, subject: String, content: String): IO[Unit] = ???

          def sendPasswordRecoveryEmail(to: Email, token: String): IO[Unit] = ???
        }

        (for {
          program <- AuthProgram.make[IO](
            mockUserProgram,
            mockTokenProgram,
            mockEmailProgram,
            mockConfig
          )
          changePassword <- program.changePassword(
            user.email,
            NewPasswordInfo(user.hashedPassword, Password("new password"))
          )
        } yield {
          changePassword shouldBe Left("Password mismatch, unable to update password")
        }).unsafeRunSync()
      }
    }
    it("should change the password if the old password matches") {
      forAll(usersGeneratorsWithPass) { case (user: User, password: Password) =>
        val mockUserProgram: UsersProgramAlg[IO] = new UsersProgramAlg[IO] {
          override def findUser(email: Email): IO[Option[User]] = user.some.pure[IO]

          override def createUser(user: User): IO[Email] = ???

          override def updateUser(user: User): IO[Option[User]] = user.some.pure[IO]

          override def deleteUser(email: Email): IO[Boolean] = ???
        }

        val mockTokenProgram: TokensProgramAlg[IO] = new TokensProgramAlg[IO] {
          def getToken(email: Email): IO[Option[String]] = ???

          def checkToken(email: Email, token: String): IO[Boolean] = ???
        }

        val mockEmailProgram: EmailsProgramAlg[IO] = new EmailsProgramAlg[IO] {
          def sendEmail(to: Email, subject: String, content: String): IO[Unit] = ???

          def sendPasswordRecoveryEmail(to: Email, token: String): IO[Unit] = ???
        }

        (for {
          program <- AuthProgram.make[IO](
            mockUserProgram,
            mockTokenProgram,
            mockEmailProgram,
            mockConfig
          )
          changePassword <- program.changePassword(
            user.email,
            NewPasswordInfo(password, Password("new password"))
          )
        } yield {
          changePassword shouldBe a[Right[_, _]]
          changePassword.value shouldBe a[Some[User]]
        }).unsafeRunSync()
      }
    }
    it("should delete a user if it exists") {
      forAll(usersGeneratorsWithPass) { case (user: User, password: Password) =>
        val mockUserProgram: UsersProgramAlg[IO] = new UsersProgramAlg[IO] {
          override def findUser(email: Email): IO[Option[User]] = ???

          override def createUser(user: User): IO[Email] = ???

          override def updateUser(user: User): IO[Option[User]] = ???

          override def deleteUser(email: Email): IO[Boolean] = true.pure[IO]
        }

        val mockTokenProgram: TokensProgramAlg[IO] = new TokensProgramAlg[IO] {
          def getToken(email: Email): IO[Option[String]] = ???

          def checkToken(email: Email, token: String): IO[Boolean] = ???
        }

        val mockEmailProgram: EmailsProgramAlg[IO] = new EmailsProgramAlg[IO] {
          def sendEmail(to: Email, subject: String, content: String): IO[Unit] = ???

          def sendPasswordRecoveryEmail(to: Email, token: String): IO[Unit] = ???
        }

        (for {
          program <- AuthProgram.make[IO](
            mockUserProgram,
            mockTokenProgram,
            mockEmailProgram,
            mockConfig
          )
          delete <- program.deleteUser(user.email)
        } yield {
          delete shouldBe true
        }).unsafeRunSync()
      }
    }
    it("should not delete a user if it doesn't exists") {
      forAll(usersGeneratorsWithPass) { case (user: User, password: Password) =>
        val mockUserProgram: UsersProgramAlg[IO] = new UsersProgramAlg[IO] {
          override def findUser(email: Email): IO[Option[User]] = ???

          override def createUser(user: User): IO[Email] = ???

          override def updateUser(user: User): IO[Option[User]] = ???

          override def deleteUser(email: Email): IO[Boolean] = false.pure[IO]
        }

        val mockTokenProgram: TokensProgramAlg[IO] = new TokensProgramAlg[IO] {
          def getToken(email: Email): IO[Option[String]] = ???

          def checkToken(email: Email, token: String): IO[Boolean] = ???
        }

        val mockEmailProgram: EmailsProgramAlg[IO] = new EmailsProgramAlg[IO] {
          def sendEmail(to: Email, subject: String, content: String): IO[Unit] = ???

          def sendPasswordRecoveryEmail(to: Email, token: String): IO[Unit] = ???
        }

        (for {
          program <- AuthProgram.make[IO](
            mockUserProgram,
            mockTokenProgram,
            mockEmailProgram,
            mockConfig
          )
          delete <- program.deleteUser(user.email)
        } yield {
          delete shouldBe false
        }).unsafeRunSync()
      }
    }
  }

}
