package com.github.dpratt747
package jobsboard.core.program

import jobsboard.core.repository.TokensRepositoryAlg
import jobsboard.domain.job.Email.Email

import cats.Applicative
import cats.effect.MonadCancelThrow
import cats.implicits.*
import org.typelevel.log4cats.Logger

trait TokensProgramAlg[F[_]] {
  def getToken(email: Email): F[Option[String]]
  def checkToken(email: Email, token: String): F[Boolean]
}
final case class TokensProgram[F[_]: MonadCancelThrow: Logger] private (
    tokensRepository: TokensRepositoryAlg[F]
) extends TokensProgramAlg[F] {
  override def getToken(email: Email): F[Option[String]] = for {
    _     <- Logger[F].info(s"Getting token for email: $email")
    token <- tokensRepository.getToken(email)
  } yield token

  override def checkToken(email: Email, token: String): F[Boolean] = for {
    _   <- Logger[F].info(s"Checking token for email: $email")
    res <- tokensRepository.checkToken(email, token)
  } yield res

}

object TokensProgram {
  def make[F[_]: MonadCancelThrow: Logger](
      tokensRepository: TokensRepositoryAlg[F]
  ): F[TokensProgramAlg[F]] =
    TokensProgram[F](tokensRepository).pure[F]
}
