package com.github.dpratt747
package playground

import jobsboard.core.repository.JobsRepository
import jobsboard.domain.job.*

import cats.effect.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object JobsPlayground extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val transactor: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](32)
    trans <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5432/board",
      "docker",
      "docker",
      ec
    )
  } yield trans

  private val jobInfo = JobInfo.minimal(
    company = CompanyName("Google"),
    title = Title("Software Engineer"),
    description = Description("Write code"),
    externalUrl = URL("https://google.com"),
    remote = false,
    location = Location("Mountain View, CA")
  )

  override def run: IO[Unit] = transactor.use { xa =>
    for {
      repo <- JobsRepository.make[IO](xa)
      id   <- repo.createJob(Email("dpratt747@gmail.com"), jobInfo)
      id2  <- repo.createJob(Email("anotheremail@valid.com"), jobInfo)
      _    <- repo.find(id).map(println)
      _    <- repo.all().map(println)
      _    <- repo.delete(id)
    } yield ()
  }

}
