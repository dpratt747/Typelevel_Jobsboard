package com.github.dpratt747
package jobsboard.core.repository

import jobsboard.domain.job.*
import jobsboard.domain.job.CompanyName.*
import jobsboard.domain.job.Country.Country
import jobsboard.domain.job.Email.Email
import jobsboard.domain.job.JobId.JobId
import jobsboard.domain.pagination.*
import jobsboard.domain.user.User
import jobsboard.logging.*

import cats.Applicative
import cats.effect.kernel.MonadCancelThrow
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.typelevel.log4cats.Logger


trait UsersRepositoryAlg[F[_]] {
  def find(email: Email): F[Option[User]]

  def create(user: User): F[Email]

  def update(user: User): F[Option[User]]

  def delete(email: Email): F[Boolean]
}

final case class UsersRepository[F[_] : MonadCancelThrow : Logger] private(
                                                                            private val xa: Transactor[F]
                                                                          ) extends UsersRepositoryAlg[F] {
  override def find(email: Email): F[Option[User]] =
    sql"""
         | SELECT email, hashed_password, first_name, last_name, company, role
         | FROM users
         | WHERE email = $email
        """.stripMargin
      .query[User]
      .option
      .transact(xa)

  override def create(user: User): F[Email] =
    sql"""
         | INSERT INTO users (email, hashed_password, first_name, last_name, company, role)
         | VALUES (${user.email}, ${user.hashedPassword}, ${user.firstName}, ${user.lastName}, ${user.company}, ${user.role})
       """.stripMargin
      .update
      .run
      .transact(xa)
      .map(_ => user.email)


  override def update(user: User): F[Option[User]] = {
    val update =
      sql"""
           | UPDATE users
           |  SET hashed_password = ${user.hashedPassword}, first_name = ${user.firstName}, last_name = ${user.lastName}, company = ${user.company}, role = ${user.role}
           |  WHERE email = ${user.email}
       """.stripMargin
        .update
        .run
        .transact(xa)

    for {
      _ <- update
      user <- find(user.email)
    } yield user
  }

  override def delete(email: Email): F[Boolean] =
    sql"""
         | DELETE FROM users
         | WHERE email = $email
        """.stripMargin
      .update
      .run
      .transact(xa)
      .map(_ > 0)
}

object UsersRepository {
  def make[F[_] : MonadCancelThrow : Logger](xa: Transactor[F]): F[UsersRepositoryAlg[F]] = UsersRepository[F](xa).pure[F]
}