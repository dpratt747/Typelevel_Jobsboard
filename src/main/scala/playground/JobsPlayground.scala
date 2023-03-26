package com.github.dpratt747
package playground

import cats.effect.*
import com.github.dpratt747.jobsboard.core.repository.JobsRepository
import com.github.dpratt747.jobsboard.domain.job.*
import doobie.hikari.HikariTransactor
import doobie.util.*

object JobsPlayground extends IOApp.Simple {

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
      id <- repo.createJob(Email("dpratt747@gmail.com"), jobInfo)
    } yield ()
  }

}
