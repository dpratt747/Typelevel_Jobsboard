package com.github.dpratt747
package jobsboard.core.program

import jobsboard.core.repository.{JobsRepositoryAlg, UsersRepositoryAlg}
import jobsboard.domain.job.*
import jobsboard.domain.job.Email.*
import jobsboard.domain.job.JobId.*
import jobsboard.domain.pagination.Pagination
import jobsboard.domain.user.User

import cats.implicits.*
import cats.{Applicative, Monad}
import org.typelevel.log4cats.Logger

trait UsersProgramAlg[F[_]] {
  def findUser(email: Email): F[Option[User]]

  def createUser(user: User): F[Email]

  def updateUser(user: User): F[Option[User]]

  def deleteUser(email: Email): F[Boolean]
}

final case class UsersProgram[F[_] : Logger : Monad] private(
                                                             private val usersRepositoryAlg: UsersRepositoryAlg[F]
                                                           ) extends UsersProgramAlg[F] {
  override def findUser(email: Email): F[Option[User]] =
    for {
      _ <- Logger[F].info(s"Attempting to find user")
      jobId <- usersRepositoryAlg.find(email)
    } yield jobId

  override def createUser(user: User): F[Email] = {
    for {
      _ <- Logger[F].info(s"Attempting to create user")
      jobs <- usersRepositoryAlg.create(user)
    } yield jobs
  }

  override def updateUser(user: User): F[Option[User]] = {
    for {
      _ <- Logger[F].info(s"Attempting to update user")
      jobs <- usersRepositoryAlg.update(user)
    } yield jobs
  }

  override def deleteUser(email: Email): F[Boolean] = {
    for {
      _ <- Logger[F].info(s"Attempting to delete user")
      jobs <- usersRepositoryAlg.delete(email)
    } yield jobs
  }
}

object UsersProgram {
  def make[F[_] : Monad : Logger](repo: UsersRepositoryAlg[F]): F[UsersProgramAlg[F]] = UsersProgram(repo).pure[F]
}
