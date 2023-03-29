package com.github.dpratt747
package jobsboard.core.repository

import cats.effect.*
import cats.effect.kernel.Resource
import doobie.{ExecutionContexts, HC, Transactor}
import doobie.hikari.HikariTransactor
import org.testcontainers.containers.PostgreSQLContainer


trait DoobieSpec {

  val initScript: String

  val postgresResource: Resource[IO, PostgreSQLContainer[Nothing]] = {
    val acquire = {
      val container: PostgreSQLContainer[Nothing] = new PostgreSQLContainer("postgres:latest").withInitScript("sql/init.sql")
      IO{
        container.start()
        container
      }
    }
    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())
    Resource.make(acquire)(release)
  }

  val rollbackTransactor: Resource[IO, Transactor[IO]] = for {
    container <- postgresResource
    ec <- ExecutionContexts.fixedThreadPool[IO](1)
    trans <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      container.getJdbcUrl,
      container.getUsername,
      container.getPassword,
      ec
    )
  } yield Transactor.after.set(trans, HC.rollback)

}
