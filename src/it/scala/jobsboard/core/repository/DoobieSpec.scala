package com.github.dpratt747
package jobsboard.core.repository

import cats.effect.*
import cats.effect.kernel.Resource
import doobie.{ExecutionContexts, HC, Transactor}
import doobie.hikari.HikariTransactor
import org.testcontainers.containers.PostgreSQLContainer

trait DoobieSpec {

  val initScript: String

  private def postgresResource: Resource[IO, PostgreSQLContainer[Nothing]] = {
    val acquire = {
      val container: PostgreSQLContainer[Nothing] = new PostgreSQLContainer("postgres")
        .withInitScript(initScript)
      IO {
        container.start()
        container
      }
    }
    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())
    Resource.make(acquire)(release)
  }

  def transactor: Resource[IO, Transactor[IO]] = for {
    container <- postgresResource
    ec        <- ExecutionContexts.fixedThreadPool[IO](1)
    trans <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      container.getJdbcUrl,
      container.getUsername,
      container.getPassword,
      ec
    )
  } yield trans

}
