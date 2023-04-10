package com.github.dpratt747
package jobsboard.http.routes

import jobsboard.core.program.{AuthProgramAlg, JobsProgram, JobsProgramAlg}
import jobsboard.domain.auth.{LoginInfo, NewPasswordInfo}
import jobsboard.domain.job.*
import jobsboard.domain.job.Email.Email
import jobsboard.domain.job.JobId.*
import jobsboard.domain.pagination.*
import jobsboard.domain.security.{Authenticator, *}
import jobsboard.domain.user.{NewUserInfo, User}
import jobsboard.http.responses.*
import jobsboard.logging.*

import cats.*
import cats.effect.*
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.parser.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.Auth
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.*
import tsec.mac.jca.HMACSHA256

import scala.collection.mutable

trait AuthRoutesAlg[F[_]] {
  def routes: HttpRoutes[F]
}

final case class AuthRoutes[F[_]: Concurrent: Logger] private (
    private val authProgram: AuthProgramAlg[F],
    private val authenticator: Authenticator[F]
) extends AuthRoutesAlg[F]
    with Http4sDsl[F] {

  private val securedRequestHandler = SecuredRequestHandler(authenticator)

  // Post /auth/login
  private def loginRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    val jwtTokenO: F[Option[AugmentedJWT[Crypto, Email]]] = for {
      loginRequest <- req.as[LoginInfo]
      token        <- authProgram.login(loginRequest.email, loginRequest.password)
    } yield token

    jwtTokenO.map {
      case Some(token) => authenticator.embed(Response(Status.Ok), token)
      case None        => Response(Status.Unauthorized)
    }
  }

  // POST /auth/users
  private def createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "user" =>
      for {
        newUser <- req.as[NewUserInfo]
        _       <- Logger[F].info(s"New user request: $newUser")
        signup  <- authProgram.signUp(newUser)
        response <- signup match {
          case Some(user) => Created(user.email)
          case None =>
            BadRequest(FailureResponse(s"User with email (${newUser.email}) already exists."))
        }
      } yield response
  }

  // PUT /auth/users/password
  private def changePasswordRoute: AuthRoute[F] = {
    case req @ PUT -> Root / "user" / "password" asAuthed user =>
      for {
        newPasswordInfo  <- req.request.as[NewPasswordInfo]
        maybeUserOrError <- authProgram.changePassword(user.email, newPasswordInfo)
        resp <- maybeUserOrError match {
          case Right(Some(_)) => Ok()
          case Right(None) =>
            NotFound(FailureResponse(s"User with email (${user.email}) not found."))
          case Left(error) => Forbidden(FailureResponse(error))
        }
      } yield resp
  }

  // POST /auth/logout
  private def logoutRoute: AuthRoute[F] = { case req @ POST -> Root / "logout" asAuthed _ =>
    Ok("TODO")
    val token = req.authenticator
    for {
      _    <- authenticator.discard(token)
      resp <- Ok()
    } yield resp
  }

  private def deleteUserRoute: AuthRoute[F] = {
    case DELETE -> Root / "user" / Email(email) asAuthed user =>
      authProgram.deleteUser(email).flatMap {
        case true  => Ok()
        case false => NotFound(FailureResponse(s"User with email ($email) not deleted."))
      }
  }

  private val authedRoutes = securedRequestHandler.liftService(
    changePasswordRoute.restrictedTo(allRoles) |+|
      logoutRoute.restrictedTo(allRoles) |+|
      deleteUserRoute.restrictedTo(adminOnly)
  )

  private val unauthorizedRoutes: HttpRoutes[F] = loginRoute <+> createUserRoute

  override def routes: HttpRoutes[F] = Router(
    "/auth" -> (unauthorizedRoutes <+> authedRoutes)
  )

}

object AuthRoutes {
  def make[F[_]: Concurrent: Monad: Logger](
      authProgram: AuthProgramAlg[F],
      authenticator: Authenticator[F]
  ): F[AuthRoutesAlg[F]] =
    AuthRoutes[F](authProgram, authenticator).pure[F]

}
