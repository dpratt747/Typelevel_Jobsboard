package com.github.dpratt747
package jobsboard

import jobsboard.config.*
import jobsboard.core.program.*
import jobsboard.core.repository.*
import jobsboard.domain.job.Email.Email
import jobsboard.domain.security.Authenticator
import jobsboard.domain.user.User
import jobsboard.http.HttpApi
import jobsboard.http.routes.HealthRoutes

import cats.data.OptionT
import cats.effect.*
import cats.implicits.*
import doobie.hikari.*
import doobie.util.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256

object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val transactorWithConfig: Resource[IO, (HikariTransactor[IO], ApplicationConfig)] = for {
    config <- Resource.eval(ConfigSource.default.loadF[IO, ApplicationConfig])
    ec     <- ExecutionContexts.fixedThreadPool[IO](config.postgresConfig.numberOfThreads)
    trans <- HikariTransactor.newHikariTransactor[IO](
      config.postgresConfig.driver,
      config.postgresConfig.url,
      config.postgresConfig.user,
      config.postgresConfig.password,
      ec
    )
  } yield (trans, config)

  override def run: IO[Unit] =
    transactorWithConfig.use { (xa, config) =>
      for {
        jobsRepo: JobsRepositoryAlg[IO]   <- JobsRepository.make[IO](xa)
        usersRepo: UsersRepositoryAlg[IO] <- UsersRepository.make[IO](xa)
        tokensRepo: TokensRepositoryAlg[IO] <- TokensRepository
          .make[IO](xa, usersRepo, config.tokenConfig)

        jobsProgram: JobsProgramAlg[IO]    <- JobsProgram.make[IO](jobsRepo)
        tokenProgram: TokensProgramAlg[IO] <- TokensProgram.make[IO](tokensRepo)
        userProgram: UsersProgramAlg[IO]   <- UsersProgram.make[IO](usersRepo)
        emailProgram: EmailsProgramAlg[IO] <- EmailsProgram.make[IO](config.emailServiceConfig)
        authProgram: AuthProgramAlg[IO] <- AuthProgram
          .make[IO](userProgram, tokenProgram, emailProgram, config)

        httpApp <- HttpApi.make[IO](jobsProgram, authProgram, authProgram.authenticator)
        routes  <- httpApp.routes
        server <- EmberServerBuilder
          .default[IO]
          .withHost(config.emberConfig.host)
          .withPort(config.emberConfig.port)
          .withHttpApp(routes.orNotFound)
          .build
          .use(_ => Logger[IO].info("Started server") *> IO.never)
      } yield server
    }

}
