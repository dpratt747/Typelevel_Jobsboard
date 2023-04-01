package com.github.dpratt747
package jobsboard.core.repository

import jobsboard.domain.job.*
import jobsboard.domain.pagination.*
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

class UsersSpec extends AnyFunSpec with Matchers with DoobieSpec with UsersGenerators with ScalaCheckPropertyChecks {
  override val initScript: String = "sql/users.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  describe("UsersRepository") {
    it("create a user") {
      forAll(usersGenerators) { user =>
        transactor.use { xa =>
          for {
            repo <- UsersRepository.make(xa)
            email <- repo.create(user)
            count <- sql"""SELECT COUNT(*) FROM users""".query[Int].unique.transact(xa)
          } yield {
            email shouldBe user.email
            count shouldBe 1
          }
        }.unsafeRunSync()
      }
    }
    it("should not create a user if the email already exists") {
      forAll(usersGenerators, usersGenerators, emailGen) { (user1, user2, email) =>
        transactor.use { xa =>
          for {
            repo <- UsersRepository.make(xa)
            insertOne <- repo.create(user1.copy(email = email))
            error <- repo.create(user2.copy(email = email)).attempt
            count <- sql"""SELECT COUNT(*) FROM users""".query[Int].unique.transact(xa)
          } yield {
            error shouldBe a[Left[Throwable, Email.Email]]
            insertOne shouldBe email
            count shouldBe 1
          }
        }.unsafeRunSync()
      }
    }
    it("should find a user by email") {
      forAll(usersGenerators) { user =>
        transactor.use { xa =>
          for {
            repo <- UsersRepository.make(xa)
            _ <- repo.create(user)
            found <- repo.find(user.email)
          } yield {
            found shouldBe Some(user)
          }
        }.unsafeRunSync()
      }
    }
    it("should return none when no user is found") {
      forAll(usersGenerators) { user =>
        transactor.use { xa =>
          for {
            repo <- UsersRepository.make(xa)
            found <- repo.find(user.email)
          } yield {
            found shouldBe None
          }
        }.unsafeRunSync()
      }
    }
    it("should update a user") {
      forAll(usersGenerators, usersGenerators) { (user1, user2) =>
        transactor.use { xa =>
          for {
            repo <- UsersRepository.make(xa)
            _ <- repo.create(user1.copy(email = user2.email))
            updated <- repo.update(user2)
            found <- repo.find(user2.email)
          } yield {
            found shouldBe Some(user2)
            updated shouldBe Some(user2)
          }
        }.unsafeRunSync()
      }
    }
    it("should not update if the user does not exist"){
      forAll(usersGenerators) { user =>
        transactor.use { xa =>
          for {
            repo <- UsersRepository.make(xa)
            updated <- repo.update(user)
          } yield {
            updated shouldBe None
          }
        }.unsafeRunSync()
      }
    }
    it("should delete a user") {
      forAll(usersGenerators) { user =>
        transactor.use { xa =>
          for {
            repo <- UsersRepository.make(xa)
            _ <- repo.create(user)
            deleted <- repo.delete(user.email)
            found <- repo.find(user.email)
          } yield {
            found shouldBe None
            deleted shouldBe true
          }
        }.unsafeRunSync()
      }
    }
    it("should not delete a user if the user does not exist") {
      forAll(emailGen) { email =>
        transactor.use { xa =>
          for {
            repo <- UsersRepository.make(xa)
            deleted <- repo.delete(email)
          } yield {
            deleted shouldBe false
          }
        }.unsafeRunSync()
      }
    }
  }

}
