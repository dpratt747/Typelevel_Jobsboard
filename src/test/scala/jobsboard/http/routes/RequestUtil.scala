package com.github.dpratt747
package jobsboard.http.routes

import jobsboard.domain.job.Email.Email

import cats.effect.IO
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, Request}
import tsec.authentication.AugmentedJWT
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

trait RequestUtil {
  extension (req: Request[IO]) {
    def withBearerToken(token: AugmentedJWT[HMACSHA256, Email]): Request[IO] = req.putHeaders {
      val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](token.jwt)

      Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
    }
  }
}
