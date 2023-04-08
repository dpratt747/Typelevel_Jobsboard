package com.github.dpratt747
package jobsboard.domain

import jobsboard.domain.job.Email.Email
import jobsboard.domain.user.*

import cats.*
import cats.implicits.*
import org.http4s.{Response, Status}
import tsec.authentication.*
import tsec.authorization.*
import tsec.mac.jca.HMACSHA256

package object security {
  type Crypto = HMACSHA256
  type JwtToken = AugmentedJWT[Crypto, Email]
  type Authenticator[F[_]] = JWTAuthenticator[F, Email, User, Crypto]
  type AuthRoute[F[_]] = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
  type AuthRBAC[F[_]] = BasicRBAC[F, Role, User, JwtToken]
  given authRole[F[_]: Applicative]: AuthorizationInfo[F, Role, User] with {
    override def fetchInfo(user: User): F[Role] = user.role.pure[F]
  }
  def allRoles[F[_]: MonadThrow]: AuthRBAC[F] = BasicRBAC.all[F, Role, User, JwtToken]

  def adminOnly[F[_]: MonadThrow]: AuthRBAC[F] = BasicRBAC(Role.ADMIN)

  def recruiterOnly[F[_]: MonadThrow]: AuthRBAC[F] = BasicRBAC(Role.RECRUITER)

  final case class Authorizations[F[_]](rbacRoutes: Map[AuthRBAC[F], List[AuthRoute[F]]])

  object Authorizations {
    given auth2tsec[F[_] : Monad]: Conversion[Authorizations[F], TSecAuthService[User, JwtToken, F]] =
      authz => {
        val unauthorisedService: TSecAuthService[User, JwtToken, F] = TSecAuthService[User, JwtToken, F] { aa =>
          Response[F](status = Status.Unauthorized).pure[F]
        }

        authz.rbacRoutes.toSeq.foldLeft(unauthorisedService) { case (acc, (rbac, routes)) =>
          val bigRoute = routes.reduce(_.orElse(_))
          TSecAuthService.withAuthorizationHandler(rbac)(bigRoute, acc.run)
        }
      }

    given semigroup[F[_]]: Semigroup[Authorizations[F]] = Semigroup.instance{ (a, b) => Authorizations(a.rbacRoutes |+| b.rbacRoutes) }
  }
  extension [F[_]](authRoute: AuthRoute[F]) {
    def restrictedTo(rbac: AuthRBAC[F]): Authorizations[F] = Authorizations(Map(rbac -> List(authRoute)))
  }

}
