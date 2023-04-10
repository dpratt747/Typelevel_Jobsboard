package com.github.dpratt747
package jobsboard.fixtures

import jobsboard.domain.job.*
import jobsboard.domain.job.CompanyName.CompanyName
import jobsboard.domain.job.Country.Country
import jobsboard.domain.job.Currency.Currency
import jobsboard.domain.job.Description.Description
import jobsboard.domain.job.FirstName.FirstName
import jobsboard.domain.job.Image.Image
import jobsboard.domain.job.JobId.JobId
import jobsboard.domain.job.LastName.LastName
import jobsboard.domain.job.Location.Location
import jobsboard.domain.job.Other.Other
import jobsboard.domain.job.Password.Password
import jobsboard.domain.job.Seniority.Seniority
import jobsboard.domain.job.Tags.Tags
import jobsboard.domain.job.Title.Title
import jobsboard.domain.job.URL.URL
import jobsboard.domain.user.*

import org.scalacheck.Gen
import tsec.passwordhashers.jca.BCrypt

trait UsersGenerators extends JobGenerators {

  val passwordGen: Gen[Password] = nonEmptyStringGen.map(Password(_))

  val firstNameGen: Gen[FirstName] = nonEmptyStringGen.map(FirstName(_))

  val lastNameGen: Gen[LastName] = nonEmptyStringGen.map(LastName(_))

  val roleGen: Gen[Role] = Gen.oneOf(Role.values)

  val usersGeneratorsWithPass: Gen[(User, Password)] = for {
    email    <- emailGen
    password <- passwordGen
    hashedPassword = Password(BCrypt.hashpwUnsafe(password.value))
    firstName <- Gen.option(firstNameGen)
    lastName  <- Gen.option(lastNameGen)
    company   <- Gen.option(companyNameGen)
    role      <- roleGen
  } yield (User(email, hashedPassword, firstName, lastName, company, role), password)

  val usersGenerators: Gen[User] = for {
    email    <- emailGen
    password <- passwordGen
    hashedPassword = Password(BCrypt.hashpwUnsafe(password.value))
    firstName <- Gen.option(firstNameGen)
    lastName  <- Gen.option(lastNameGen)
    company   <- Gen.option(companyNameGen)
    role      <- roleGen
  } yield User(email, hashedPassword, firstName, lastName, company, role)

  val newUserInfoGen: Gen[NewUserInfo] = for {
    email     <- emailGen
    password  <- passwordGen
    firstName <- Gen.option(firstNameGen)
    lastName  <- Gen.option(lastNameGen)
    company   <- Gen.option(companyNameGen)
  } yield NewUserInfo(email, password, firstName, lastName, company)

}
