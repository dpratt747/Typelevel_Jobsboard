package com.github.dpratt747
package jobsboard.core.repository

import jobsboard.domain.job.Email.Email

import cats.effect.kernel.MonadCancelThrow
import doobie.Transactor
import doobie.util.transactor.Transactor
import doobie.implicits.*
import org.typelevel.log4cats.Logger
import cats.implicits.*
import com.github.dpratt747.jobsboard.config.TokenConfig

import scala.util.Random

trait TokensRepositoryAlg[F[_]] {
  def getToken(email: Email): F[Option[String]]

  def checkToken(email: Email, token: String): F[Boolean]
}

final case class TokensRepository[F[_]: MonadCancelThrow: Logger] private (
    private val xa: Transactor[F],
    private val usersRepository: UsersRepositoryAlg[F],
    private val config: TokenConfig
) extends TokensRepositoryAlg[F] {

  private def randomToken(maxLen: Int): F[String] =
    Random.alphanumeric.map(Character.toUpperCase).take(maxLen).mkString.pure[F]

  private def updateToken(email: Email): F[String] = for {
    token <- randomToken(8)
    _ <-
      sql"""
           |UPDATE recovery_tokens
           |SET token = $token, expiration = ${System.currentTimeMillis() + config.tokenDuration}
           |WHERE email = $email
             """.stripMargin.update.run.transact(xa)
  } yield token

  private def generateToken(email: Email): F[String] = for {
    token <- randomToken(8)
    _ <-
      sql"""
           |INSERT INTO recovery_tokens (email, token, expiration)
           |VALUES ($email, $token, ${System.currentTimeMillis() + config.tokenDuration})
             """.stripMargin.update.run.transact(xa)
  } yield token

  private def findToken(email: Email): F[Option[String]] =
    sql"""
         |SELECT token FROM recovery_tokens
         |WHERE email = $email
       """.stripMargin
      .query[String]
      .option
      .transact(xa)

  private def getNewToken(email: Email): F[String] = findToken(email).flatMap {
    case Some(token) => updateToken(email)
    case None        => generateToken(email)
  }

  def getToken(email: Email): F[Option[String]] =
    usersRepository.find(email).flatMap {
      case None    => None.pure[F]
      case Some(_) => getNewToken(email).map(Some(_))
    }

  def checkToken(email: Email, token: String): F[Boolean] =
    sql"""
      | SELECT token
      | FROM recovery_tokens
      | WHERE email = $email AND token = $token AND expiration > ${System.currentTimeMillis()}
       """.stripMargin
      .query[String]
      .option
      .transact(xa)
      .map(_.nonEmpty)

}

object TokensRepository {
  def make[F[_]: MonadCancelThrow: Logger](
      xa: Transactor[F],
      usersRepository: UsersRepositoryAlg[F],
      config: TokenConfig
  ): F[TokensRepositoryAlg[F]] =
    TokensRepository[F](xa, usersRepository, config).pure[F]
}
