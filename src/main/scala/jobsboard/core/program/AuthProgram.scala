package com.github.dpratt747
package jobsboard.core.program

import jobsboard.core.repository.{JobsRepositoryAlg, UsersRepositoryAlg}
import jobsboard.domain.auth.NewPasswordInfo
import jobsboard.domain.job.*
import jobsboard.domain.job.CompanyName.CompanyName
import jobsboard.domain.job.Email.*
import jobsboard.domain.job.FirstName.FirstName
import jobsboard.domain.job.JobId.*
import jobsboard.domain.job.LastName.LastName
import jobsboard.domain.job.Password.Password
import jobsboard.domain.pagination.Pagination
import jobsboard.domain.user.{Role, User}

import cats.effect.Sync
import cats.effect.kernel.Async
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import cats.{Applicative, Monad}
import org.typelevel.log4cats.Logger
import tsec.authentication.{AugmentedJWT, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

trait AuthProgramAlg[F[_]] {
  def login(email: Email, password: Password): F[Option[AugmentedJWT[HMACSHA256, Email]]]

  def signUp(email: Email, password: Password, firstName: Option[FirstName], lastName: Option[LastName], companyName: Option[CompanyName]): F[Option[User]]

  def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]] = ???
}

final case class AuthProgram[F[_] : Logger : Async] private(
                                                             private val usersRepositoryAlg: UsersRepositoryAlg[F],
                                                             private val authenticator: JWTAuthenticator[F, Email, User, HMACSHA256]
                                                           ) extends AuthProgramAlg[F] {
  override def login(email: Email, password: Password): F[Option[AugmentedJWT[HMACSHA256, Email]]] =
    for {
      userO <- usersRepositoryAlg.find(email)
      validatedO <- userO.filterA(user =>
        BCrypt.checkpwBool[F](
          password.value,
          PasswordHash[BCrypt](user.hashedPassword.value)
        )
      )
      jwtO <- validatedO.traverse(user => authenticator.create(user.email))
    } yield jwtO


  override def signUp(email: Email, password: Password, firstName: Option[FirstName], lastName: Option[LastName], companyName: Option[CompanyName]): F[Option[User]] =
    usersRepositoryAlg.find(email).flatMap {
      case Some(_) => none.pure[F]
      case None =>
        for {
          hashedPassword <- BCrypt.hashpw[F](password.value)
          user = User(email, Password(hashedPassword), firstName, lastName, companyName, Role.RECRUITER)
          _ <- usersRepositoryAlg.create(user)
        } yield Some(user)
    }


  override def changePassword(email: Email, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]] =
    usersRepositoryAlg.find(email).flatMap {
      case Some(user) =>
        for {
          validated <- BCrypt.checkpwBool[F](
            newPasswordInfo.oldPassword.value,
            PasswordHash[BCrypt](user.hashedPassword.value)
          )
          updateRes <-
            if (validated) {
              for {
                hashedPassword <- BCrypt.hashpw[F](newPasswordInfo.newPassword.value)
                userWithNewHash: User = user.copy(hashedPassword = Password(hashedPassword))
                userUpdated <- usersRepositoryAlg.update(userWithNewHash)
              } yield Right(userUpdated)
            } else {
              Left("Password mismatch, unable to update password").pure[F]
            }
        } yield updateRes
      case None => Right(None).pure[F]
    }
}

object AuthProgram {
  def make[F[_] : Logger : Async](usersRepositoryAlg: UsersRepositoryAlg[F], authenticator: JWTAuthenticator[F, Email, User, HMACSHA256]): F[AuthProgramAlg[F]] =
    AuthProgram(usersRepositoryAlg, authenticator).pure[F]
}
