package com.github.dpratt747
package jobsboard

import jobsboard.config.*
import jobsboard.core.program.JobsProgram
import jobsboard.core.repository.JobsRepository
import jobsboard.http.HttpApi
import jobsboard.http.routes.HealthRoutes

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


object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val transactor: Resource[IO, (HikariTransactor[IO], ApplicationConfig)] = for {
    config <- Resource.eval(ConfigSource.default.loadF[IO, ApplicationConfig])
    ec <- ExecutionContexts.fixedThreadPool[IO](config.postgresConfig.numberOfThreads)
    trans <- HikariTransactor.newHikariTransactor[IO](
      config.postgresConfig.driver,
      config.postgresConfig.url,
      config.postgresConfig.user,
      config.postgresConfig.password,
      ec
    )
  } yield (trans, config)
  
  override def run: IO[Unit] = {
    transactor.use { (xa, config) =>
      for {
        jobsRepo <- JobsRepository.make[IO](xa)
        jobsProgram <- JobsProgram.make[IO](jobsRepo)
        httpApp <- HttpApi.make[IO](jobsProgram)
        routes <- httpApp.routes
        server <- EmberServerBuilder.default[IO]
          .withHost(config.emberConfig.host)
          .withPort(config.emberConfig.port)
          .withHttpApp(routes.orNotFound)
          .build
          .use(_ => Logger[IO].info("Started server") *> IO.never)
      } yield server
    }
  }

}
