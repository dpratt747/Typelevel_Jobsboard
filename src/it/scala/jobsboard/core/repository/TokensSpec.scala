package com.github.dpratt747
package jobsboard.core.repository

import jobsboard.config.TokenConfig
import jobsboard.domain.job.*
import jobsboard.domain.job.Email.Email
import jobsboard.domain.pagination.*
import jobsboard.domain.user.User
import jobsboard.fixtures.{JobGenerators, UsersGenerators}

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import doobie.implicits.*
import org.scalacheck.Gen
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*

class TokensSpec
    extends AnyFunSpec
    with Matchers
    with DoobieSpec
    with UsersGenerators
    with ScalaCheckPropertyChecks {
  override val initScript: String = "sql/recoveryTokens.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  describe("TokensRepository") {
    it("should not create a new token for a non-existent user") {
      forAll(emailGen) { email =>
        val mockUserRepo: UsersRepositoryAlg[IO] = new UsersRepositoryAlg[IO] {
          def find(email: Email): IO[Option[User]] = none[User].pure[IO]

          def create(user: User): IO[Email] = ???

          def update(user: User): IO[Option[User]] = ???

          def delete(email: Email): IO[Boolean] = ???
        }

        transactor
          .use { xa =>
            for {
              repo <- TokensRepository.make[IO](xa, mockUserRepo, TokenConfig(2000L))
              res  <- repo.getToken(email)
            } yield res shouldBe None
          }
          .unsafeRunSync()
      }
    }
    it("should create a new token for an existing user") {
      forAll(emailGen, usersGenerators) { (email, user) =>
        val mockUserRepo: UsersRepositoryAlg[IO] = new UsersRepositoryAlg[IO] {
          def find(email: Email): IO[Option[User]] = user.some.pure[IO]

          def create(user: User): IO[Email] = ???

          def update(user: User): IO[Option[User]] = ???

          def delete(email: Email): IO[Boolean] = ???
        }

        transactor
          .use { xa =>
            for {
              repo <- TokensRepository.make[IO](xa, mockUserRepo, TokenConfig(2000L))
              res  <- repo.getToken(email)
            } yield res shouldBe defined
          }
          .unsafeRunSync()
      }
    }
    it("should fail to validate expired tokens") {
      forAll(emailGen, usersGenerators) { (email, user) =>
        val mockUserRepo: UsersRepositoryAlg[IO] = new UsersRepositoryAlg[IO] {
          def find(email: Email): IO[Option[User]] = user.some.pure[IO]

          def create(user: User): IO[Email] = ???

          def update(user: User): IO[Option[User]] = ???

          def delete(email: Email): IO[Boolean] = ???
        }

        transactor
          .use { xa =>
            for {
              repo  <- TokensRepository.make[IO](xa, mockUserRepo, TokenConfig(200L))
              token <- repo.getToken(email)
              _     <- IO.sleep(500.millis)
              res <- token match {
                case Some(t) => repo.checkToken(email, t)
                case None    => false.pure[IO]
              }
            } yield res shouldBe false
          }
          .unsafeRunSync()
      }
    }
    it("should successfully validate non-expired tokens") {
      forAll(emailGen, usersGenerators) { (email, user) =>
        val mockUserRepo: UsersRepositoryAlg[IO] = new UsersRepositoryAlg[IO] {
          def find(email: Email): IO[Option[User]] = user.some.pure[IO]

          def create(user: User): IO[Email] = ???

          def update(user: User): IO[Option[User]] = ???

          def delete(email: Email): IO[Boolean] = ???
        }

        transactor
          .use { xa =>
            for {
              repo  <- TokensRepository.make[IO](xa, mockUserRepo, TokenConfig(2000L))
              token <- repo.getToken(email)
              res <- token match {
                case Some(t) => repo.checkToken(email, t)
                case None    => false.pure[IO]
              }
            } yield res shouldBe true
          }
          .unsafeRunSync()
      }
    }
    it("should only validate tokens for the correct user") {
      forAll(usersGenerators, emailGen) { (user, anotherEmail) =>
        val mockUserRepo: UsersRepositoryAlg[IO] = new UsersRepositoryAlg[IO] {
          def find(email: Email): IO[Option[User]] = user.some.pure[IO]

          def create(user: User): IO[Email] = ???

          def update(user: User): IO[Option[User]] = ???

          def delete(email: Email): IO[Boolean] = ???
        }

        transactor
          .use { xa =>
            for {
              repo  <- TokensRepository.make[IO](xa, mockUserRepo, TokenConfig(2000L))
              token <- repo.getToken(user.email)
              res <- token match {
                case Some(t) => repo.checkToken(user.email, t)
                case None    => false.pure[IO]
              }
              res2 <- token match {
                case Some(t) => repo.checkToken(anotherEmail, t)
                case None    => false.pure[IO]
              }
            } yield {
              res shouldBe true
              res2 shouldBe false
            }
          }
          .unsafeRunSync()
      }
    }
  }
}
