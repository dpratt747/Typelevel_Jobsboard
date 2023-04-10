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
import jobsboard.domain.security.{Authenticator, JwtToken}
import jobsboard.domain.user.{NewUserInfo, Role, User}

import cats.effect.{Ref, Sync}
import cats.effect.kernel.Async
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import cats.{Applicative, Monad}
import com.github.dpratt747.jobsboard.config.ApplicationConfig
import org.typelevel.log4cats.Logger
import tsec.authentication.{AugmentedJWT, BackingStore, IdentityStore, JWTAuthenticator}
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

trait AuthProgramAlg[F[_]] {

  def authenticator: Authenticator[F]

  def login(email: Email, password: Password): F[Option[AugmentedJWT[HMACSHA256, Email]]]

  def signUp(user: NewUserInfo): F[Option[User]]

  def changePassword(
      email: Email,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]]

  def deleteUser(email: Email): F[Boolean]
}

final case class AuthProgram[F[_]: Logger: Async] private (
    private val usersRepositoryAlg: UsersRepositoryAlg[F],
    private val authenticatorF: Authenticator[F]
) extends AuthProgramAlg[F] {

  override def authenticator: Authenticator[F] = authenticatorF

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

  override def signUp(user: NewUserInfo): F[Option[User]] =
    usersRepositoryAlg.find(user.email).flatMap {
      case Some(_) => none.pure[F]
      case None =>
        for {
          hashedPassword <- BCrypt.hashpw[F](user.password.value)
          userItem = User(
            user.email,
            Password(hashedPassword),
            user.firstName,
            user.lastName,
            user.company,
            Role.RECRUITER
          )
          _ <- usersRepositoryAlg.create(userItem)
        } yield Some(userItem)
    }

  override def changePassword(
      email: Email,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]] =
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

  override def deleteUser(email: Email): F[Boolean] =
    usersRepositoryAlg.delete(email)

}

object AuthProgram {
  def make[F[_]: Logger: Async](
      usersRepositoryAlg: UsersRepositoryAlg[F],
      config: ApplicationConfig
  ): F[AuthProgramAlg[F]] = {

    import scala.concurrent.duration.*
    import cats.data.*
    import cats.implicits.*

    val idStore: IdentityStore[F, Email, User] = (email: Email) =>
      OptionT(usersRepositoryAlg.find(email))

    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        override def get(id: SecureRandomId): OptionT[F, JwtToken] = OptionT(ref.get.map(_.get(id)))

        override def put(elem: JwtToken): F[JwtToken] =
          ref.modify(store => (store + (elem.id -> elem), elem))

        override def delete(id: SecureRandomId): F[Unit] = ref.modify(store => (store - id, ()))

        override def update(v: JwtToken): F[JwtToken] = put(v)
      }
    }

    val keyF = HMACSHA256.buildKey[F](config.securityConfig.secret.getBytes("UTF-8"))

    for {
      tokenStore <- tokenStoreF
      key        <- keyF
      authenticator = JWTAuthenticator.backed.inBearerToken(
        expiryDuration = config.securityConfig.jwtExpiryDuration,
        maxIdle = None,
        tokenStore = tokenStore,
        identityStore = idStore,
        signingKey = key
      )
    } yield AuthProgram(usersRepositoryAlg, authenticator)

  }
}
