package com.github.dpratt747
package jobsboard

import jobsboard.config.*
import jobsboard.http.HttpApi
import jobsboard.http.routes.HealthRoutes

import cats.effect.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException


object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    for {
      config <- ConfigSource.default.loadF[IO, ApplicationConfig]
      server <- EmberServerBuilder.default[IO]
        .withHost(config.emberConfig.host)
        .withPort(config.emberConfig.port)
        .withHttpApp(HttpApi.make[IO]().routes.orNotFound)
        .build
        .use(_ => Logger[IO].info("Started server") *> IO.never)
    } yield server


}
